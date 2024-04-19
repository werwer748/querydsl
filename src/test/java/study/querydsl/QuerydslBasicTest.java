package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        // member1 찾기
        String qlString = "select m from Member m where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
//        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
//        QMember m = new QMember("m"); // alias 지정 - 같은 테이블을 죠인하는 경우가 아니면 사용할 일이 잘 없음
//        QMember m = QMember.member;

//        Member findMember = queryFactory
//                .select(QMember.member)
//                .from(QMember.member)
//                .where(QMember.member.username.eq("member1"))
//                .fetchOne();

        // 권장되는 방법(static import)
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /**
     * member.username.eq("member1") // username = 'member1'
     * member.username.ne("member1") //username != 'member1'
     * member.username.eq("member1").not() // username != 'member1'
     * member.username.isNotNull() //이름이 is not null
     * member.age.in(10, 20) // age in (10,20)
     * member.age.notIn(10, 20) // age not in (10, 20)
     * member.age.between(10,30) //between 10, 30
     * member.age.goe(30) // age >= 30
     * member.age.gt(30) // age > 30
     * member.age.loe(30) // age <= 30
     * member.age.lt(30) // age < 30
     * member.username.like("member%") //like 검색
     * member.username.contains("member") // like ‘%member%’ 검색
     * member.username.startsWith("member") //like ‘member%’ 검색
     */

    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member) // selectFrom? select + from
                .where(member.username.eq("member1").and(member.age.between(10, 30)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member) // selectFrom? select + from
//                .where(member.username.eq("member1").and(member.age.between(10, 30)))
                .where(
                        member.username.eq("member1"), // and를 이런식으로 쓸 수 있다.
                        member.age.between(10, 30)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /**
     * `fetch()` : 리스트 조회, 데이터 없으면 빈 리스트 반환
     * `fetchOne()` : 단 건 조회
     * 결과가 없으면 : `null`
     * 결과가 둘 이상이면 : `com.querydsl.core.NonUniqueResultException`
     * `fetchFirst()` : `limit(1).fetchOne()`
     * `fetchResults()` : 페이징 정보 포함, total count 쿼리 추가 실행 - deprecated
     * `fetchCount()` : count 쿼리로 변경해서 count 수 조회 - deprecated
     */

    @Test
    public void resultFetch() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst(); // limit(1).fetchOne()

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults(); // deprecated

        results.getTotal();
        List<Member> content = results.getResults();

        long total = queryFactory
                .selectFrom(member)
                .fetchCount(); // deprecated
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
//                .where(member.age.gt(90), member.username.isNotNull())
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .orderBy(member.age.desc(), member.username.asc())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
//        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result).hasSize(2);
    }

    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults()).hasSize(2);
    }

    @Test
    public void aggregation() {
        //? Tuple: queryDsl이 제공하는 튜플
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령구하기
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team) //? 기본적으로 on member.team_id = team.id 매칭
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) // leftJoin, rightJoin 등 모두 가능
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인: 연관관계가 없는 필드로 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team) // from절에 2개 테이블을 나열
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA")) // 기본 id 매칭에 팀이름 조건이 +
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team) // 기본 id매칭을 뺀것. member.team, team 을 해야 기본 id 매칭
                .on(member.username.eq(team.name))  // 전체팀을 가져와서 맴버이름과 팀이름이 같은 팀만 죠인
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    // 증명용
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // 초기화된 엔티티인지 안된 엔티티인지 알려줌
        assertThat(loaded).as("패치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // 초기화된 엔티티인지 안된 엔티티인지 알려줌
        assertThat(loaded).as("패치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원을 조회
     */
    @Test
    public void subQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq( // 멤버 나이 = 서브쿼리로 가져온 제일 많은 나이
                        // 제일많은 나이를 서브쿼리로 가져옴
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        for (Member m : result) { // 결과는 1개 맞음
            System.out.println("m = " + m);
        }

        assertThat(result)
                .extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원을 조회
     */
    @Test
    public void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        for (Member m : result) { // 결과는 2개 맞음
            System.out.println("m = " + m);
        }

        assertThat(result)
                .extracting("age")
                .contains(40, 30);
    }

    /**
     * In!
     */
    @Test
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        for (Member m : result) { // 결과는 3개 맞음
            System.out.println("m = " + m);
        }

        assertThat(result)
                .extracting("age")
                .contains(20, 40, 30);
    }

    // * select절 subquery
    @Test
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");
        Expression<Double> ageAvg = ExpressionUtils.as(
                select(memberSub.age.avg())
                        .from(memberSub),
                "ageAvg"
        );

        List<Tuple> result = queryFactory
                .select(
                        member.username,
                        ageAvg
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
            System.out.println("tuple = " + tuple.get(member.username));
            System.out.println("tuple = " + tuple.get(1, Double.class));
            System.out.println("tuple = " + tuple.get(ageAvg));
        }

    }

    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(
                        member.age
                                .when(10).then("열살")
                                .when(20).then("스무살")
                                .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void orderCase() {
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);
            System.out.println("username = " + username + " age = " + age + " rank = " + rank);
        }
    }

    @Test
    public void findByDistinct() {
        Member member5 = new Member("member4", 50);
        em.persist(member5);

        List<String> result = queryFactory
                .select(member.username).distinct()
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(
                        member.username,
                        Expressions.constant("A")
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() {
        // username_age를 하려고 함.
        List<String> result = queryFactory
                // stringValue: 문자가 아닌값을 처리할 때 자주 사용하고 특히 ENUM을 처리할 때 유용하다고 함.
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        // Tuple이 레포지토리 계층을 넘어가는건 좋지 않다.
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDtoByJPQL() {
        // jpql이 제공하는 new operation을 활용하는 방법이라고 함.
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                // Projections.bean : Setter를 활용함.
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age)
                )
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryFactory
                // Projections.fields : Getter, Setter 필요 없음
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age)
                )
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByConstructor() {
//        List<MemberDto> result = queryFactory
        List<UserDto> result = queryFactory
                // Projections.constructor : 생성자를 활용 함. - 맞는 타입을 정확한 위치에 놔줘야 함
                //! 값이 잘못 입력된 경우 컴파일 단계에서는 잡아내지 못한다.
//                .select(Projections.constructor(MemberDto.class,
                .select(Projections.constructor(UserDto.class, //? 별칭이 달라도 타입만 맞으면 알아서 들어간다!
                                member.username,
                                member.age
//                        member.id
                        )
                )
                .from(member)
                .fetch();

//        for (MemberDto memberDto : result) {
//            System.out.println("memberDto = " + memberDto);
//        }
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findUserDto() { // 별칭이 다른 경우
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
//                        member.username.as("name"), // 별칭을 맞춰줌.
                                ExpressionUtils.as(member.username, "name"), // 이렇게도 사용 가능
                                ExpressionUtils.as( // 서브쿼리에 별칭을 줘서 별칭을 맞춘다.
                                        JPAExpressions
                                                .select(memberSub.age.max())
                                                .from(memberSub), "age"
                                ))
                )
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * Dto 생성자에 @QueryProjection을 붙이면 컴파일 단계에 에러를 잡을 수 있는 다음과 같은 형태로 사용 가능
     * new QMemberDto(member.username, member.age)
     * 단 Dto가 QueryDsl에 의존성이 생김..
     * Dto는 Controller, Service, Repository등 다양한 곳에서 사용되는데 순수한 상태로 사용할 수가 없게 됨.
     */
    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null; //10;

        List<Member> result = searchMember1(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
//        BooleanBuilder builder = new BooleanBuilder(); // 기본 사용법
        BooleanBuilder builder = new BooleanBuilder(
                member.username.eq(usernameCond)
        ); // 무조건 넘어오는 값이 있을 경우 초기값 주기
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQuery_whereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
//                .where(usernameEq(usernameCond), ageEq(ageCond)) // where에 null이 들어가면 그냥 무시되고 다음 조건 서칭
                .where(allEq(usernameCond, ageCond)) // 조립하여 사용하는 경우
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) { // 기본적으로 Predicate를 사용하지만 조립해서 사용할 경우 BooleanExpression 사용
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer age) {
        return age != null ? member.age.eq(age) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
//    @Commit // 결과 확인용
    public void bulkUpdate() {
        /**
         * 이런식의 bulk가 실행될 경우 영속성 컨텍스트와 DB의 값에 차이가 생긴다.
         * 쿼리가 영속성컨텍스트를 무시하고 실행되버리기 때문.
         */
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        /**
         * 여기서 값을 다시 가지고 왔을 때 영속성 컨텍스트는 바뀌기 전값을 가지고있음
         * 이 경우 영속성컨텍스트에 새로 가지고온값을 담으려고해도 기존값이 있기 때문에 담기지가 않음
         * 해결법 flush, clear
         */
        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();
        for (Member m : result) {
            System.out.println("m = " + m);
        }
    }

    @Test
//    @Commit
    public void bulkAdd() {
        long count = queryFactory
                .update(member)
//                .set(member.age, member.age.multiply(2))
                .set(member.age, member.age.add(30))
                .execute();
    }

    @Test
    public void bulkDelete() {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    public void sqlFunction() {
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate(
                                "function('replace', {0}, {1}, {2})",
                                member.username, "member", "M"
                        )
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    @Test
    public void sqlFunction2() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate(
//                                "function('lower', {0})",
//                                member.username
//                        )
//                ))
                .where(member.username.eq(
                           member.username.lower() // 왠만한 표준 기술은 내장되어있음
                        )
                )
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}
