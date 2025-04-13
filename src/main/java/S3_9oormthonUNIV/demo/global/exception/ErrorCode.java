package S3_9oormthonUNIV.demo.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    EMPTY_FILE_EXCEPTION("업로드할 파일이 비어 있습니다."),
    NO_FILE_EXTENTION("파일 확장자가 존재하지 않습니다."),
    INVALID_FILE_EXTENTION("지원하지 않는 파일 확장자입니다. (jpg, jpeg, png, gif만 가능)"),
    IO_EXCEPTION_ON_IMAGE_UPLOAD("이미지 업로드 중 IOException이 발생했습니다."),
    IO_EXCEPTION_ON_IMAGE_DELETE("이미지 삭제 중 오류가 발생했습니다."),
    PUT_OBJECT_EXCEPTION("S3에 객체를 업로드하는 도중 오류가 발생했습니다.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }
}