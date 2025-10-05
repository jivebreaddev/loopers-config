package com.loopers.integration;

import com.loopers.domain.example.ExampleModel;
import com.loopers.infrastructure.example.ExampleJpaRepository;
import com.loopers.testcontainers.MySqlTestContainersConfig;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(MySqlTestContainersConfig.class)
class MySqlIntegrationTest {

    @Autowired
    private ExampleJpaRepository exampleJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("MySQL에 ExampleModel을 저장하고 조회할 수 있다")
    void saveAndFind() {
        // given
        ExampleModel example = new ExampleModel("테스트 제목", "테스트 설명");

        // when
        ExampleModel saved = exampleJpaRepository.save(example);
        Optional<ExampleModel> found = exampleJpaRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getName()).isEqualTo("테스트 제목");
        assertThat(found.get().getDescription()).isEqualTo("테스트 설명");
    }

    @Test
    @DisplayName("MySQL에서 ExampleModel을 수정할 수 있다")
    void update() {
        // given
        ExampleModel example = new ExampleModel("원본 제목", "원본 설명");
        ExampleModel saved = exampleJpaRepository.save(example);

        // when
        saved.updateName("수정된 제목");
        saved.updateDescription("수정된 설명");
        exampleJpaRepository.save(saved);

        // then
        Optional<ExampleModel> updated = exampleJpaRepository.findById(saved.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("수정된 제목");
        assertThat(updated.get().getDescription()).isEqualTo("수정된 설명");
    }

    @Test
    @DisplayName("MySQL에서 ExampleModel을 삭제할 수 있다")
    void delete() {
        // given
        ExampleModel example = new ExampleModel("삭제할 제목", "삭제할 설명");
        ExampleModel saved = exampleJpaRepository.save(example);

        // when
        exampleJpaRepository.delete(saved);

        // then
        Optional<ExampleModel> deleted = exampleJpaRepository.findById(saved.getId());
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("MySQL에서 모든 ExampleModel을 조회할 수 있다")
    void findAll() {
        // given
        exampleJpaRepository.save(new ExampleModel("예시1", "설명1"));
        exampleJpaRepository.save(new ExampleModel("예시2", "설명2"));
        exampleJpaRepository.save(new ExampleModel("예시3", "설명3"));

        // when
        var allExamples = exampleJpaRepository.findAll();

        // then
        assertThat(allExamples).hasSize(3);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 빈 Optional을 반환한다")
    void findByIdNotFound() {
        // given
        Long nonExistentId = 999L;

        // when
        Optional<ExampleModel> result = exampleJpaRepository.findById(nonExistentId);

        // then
        assertThat(result).isEmpty();
    }
}
