package study.querydsl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QuerydslApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuerydslApplication.class, args);
    }

//    @Bean // QueryDsl 빈으로 등록해서 사용하기
//    JPAQueryFactory jpaQueryFactory(EntityManager em) {
//        return new JPAQueryFactory(em);
//    }

}
