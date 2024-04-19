package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

    @QueryProjection // Dto를 Q파일로 만들어 준다.
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
