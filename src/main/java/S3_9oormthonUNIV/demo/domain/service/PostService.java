package S3_9oormthonUNIV.demo.domain.service;


import S3_9oormthonUNIV.demo.domain.entity.Post;
import S3_9oormthonUNIV.demo.domain.repository.PostRepository;
import S3_9oormthonUNIV.demo.global.exception.ErrorCode;
import S3_9oormthonUNIV.demo.global.exception.S3Exception;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.IOUtils;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class PostService {

    private final AmazonS3 amazonS3;
    private final PostRepository postRepository;

    @Value("${cloud.aws.s3.bucketName}")
    private String bucketName;

    public String upload(MultipartFile image) {
        if(image.isEmpty() || Objects.isNull(image.getOriginalFilename())){
            throw new S3Exception(ErrorCode.EMPTY_FILE_EXCEPTION);
        }
        return this.uploadImage(image);
    }

    private String uploadImage(MultipartFile image) {
        this.validateImageFileExtention(image.getOriginalFilename());
        try {
            return this.uploadImageToS3(image);
        } catch (IOException e) {
            throw new S3Exception(ErrorCode.IO_EXCEPTION_ON_IMAGE_UPLOAD);
        }
    }

    private void validateImageFileExtention(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new S3Exception(ErrorCode.NO_FILE_EXTENTION);
        }

        String extention = filename.substring(lastDotIndex + 1).toLowerCase();
        List<String> allowedExtentionList = Arrays.asList("jpg", "jpeg", "png", "gif");

        if (!allowedExtentionList.contains(extention)) {
            throw new S3Exception(ErrorCode.INVALID_FILE_EXTENTION);
        }
    }

    private String uploadImageToS3(MultipartFile image) throws IOException {
        String originalFilename = image.getOriginalFilename(); //원본 파일 명
        String extention = originalFilename.substring(originalFilename.lastIndexOf(".")); //확장자 명

        String s3FileName = UUID.randomUUID().toString().substring(0, 10) + originalFilename; //UUID를 통해 중복없이 변경된 파일 명

        log.info("[S3 업로드 시작] 원본파일명: {}, 확장자: {}, 저장될 S3 파일명: {}", originalFilename, extention, s3FileName);

        InputStream is = image.getInputStream();
        byte[] bytes = IOUtils.toByteArray(is);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/" + extention); // OK: image/png
        metadata.setContentLength(bytes.length);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);

        try{
            PutObjectRequest putObjectRequest =
                    new PutObjectRequest(bucketName, s3FileName, byteArrayInputStream, metadata);
                            //.withCannedAcl(CannedAccessControlList.PublicRead); ACL (Access Control List)말고 정책으로 명시 했기 때문에 이 코드는 필요 없다.

            amazonS3.putObject(putObjectRequest); // put image to S3
        }catch (Exception e){
            log.error("[S3 업로드 실패] 에러 메시지: {}", e.getMessage(), e);
            throw new S3Exception(ErrorCode.PUT_OBJECT_EXCEPTION);
        }finally {
            byteArrayInputStream.close();
            is.close();
        }

        return amazonS3.getUrl(bucketName, s3FileName).toString();
    }

    public void deleteImageFromS3(String imageAddress){
        String key = getKeyFromImageAddress(imageAddress);
        try{
            amazonS3.deleteObject(new DeleteObjectRequest(bucketName, key));
        }catch (Exception e){
            throw new S3Exception(ErrorCode.IO_EXCEPTION_ON_IMAGE_DELETE);
        }
    }

    private String getKeyFromImageAddress(String imageAddress){
        try{
            URL url = new URL(imageAddress);
            String decodingKey = URLDecoder.decode(url.getPath(), "UTF-8");
            return decodingKey.substring(1); // 맨 앞의 '/' 제거
        }catch (MalformedURLException | UnsupportedEncodingException e){
            throw new S3Exception(ErrorCode.IO_EXCEPTION_ON_IMAGE_DELETE);
        }
    }
    public void savePost(String title, String content, MultipartFile imageFile) {
        // 1. 이미지 S3 업로드
        String imageUrl = this.upload(imageFile);

        // 2. Post 객체 생성 및 저장
        Post post = Post.builder()
                .title(title)
                .content(content)
                .image(imageUrl)
                .build();

        postRepository.save(post);
    }

    @Transactional
    public void deleteImageAndUpdatePost(Long postId) {
        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new S3Exception(ErrorCode.POST_NOT_FOUND));

        // 2. 기존 이미지 주소 가져오기
        String imageUrl = post.getImage();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // 3. S3에서 이미지 삭제
            deleteImageFromS3(imageUrl);

            // 4. DB에서 이미지 필드 초기화
            post.setImage(null);
            postRepository.save(post);
        }
    }
}