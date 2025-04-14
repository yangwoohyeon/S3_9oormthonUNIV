package S3_9oormthonUNIV.demo.domain.controller;


import S3_9oormthonUNIV.demo.domain.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<String> createPost(@RequestParam("title") String title,
                                             @RequestParam("content") String content,
                                             @RequestParam("image") MultipartFile image) {
        postService.savePost(title, content, image);
        return ResponseEntity.ok("게시글이 성공적으로 저장되었습니다.");
    }

    @DeleteMapping("/image")
    public ResponseEntity<String> deleteImage(@RequestParam("imageUrl") String imageUrl) {
        postService.deleteImageFromS3(imageUrl);
        return ResponseEntity.ok("이미지가 성공적으로 삭제되었습니다.");
    }

    @DeleteMapping("image/{postId}")
    public ResponseEntity<String> deleteImage(@PathVariable Long postId) {
        postService.deleteImageAndUpdatePost(postId);
        return ResponseEntity.ok("이미지가 삭제되었습니다.");
    }
}
