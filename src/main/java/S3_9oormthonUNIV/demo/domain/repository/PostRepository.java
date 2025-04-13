package S3_9oormthonUNIV.demo.domain.repository;

import S3_9oormthonUNIV.demo.domain.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
