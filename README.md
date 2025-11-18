## 📚 데이터베이스

---

간단한 로컬 데이터베이스 프로그램을 구현한다.

프로그램 종료 후 다시 실행해도 파일 기반 영속성으로 데이터를 조회할 수 있어야 한다.

### 🗂 프로젝트 소개

- 목표: 파일에 저장되는 단일 프로세스 로컬 DB
- 특징
    - 테이블 정의 및 스키마 관리
    - 파일 기반 저장
    - B+Tree 인덱스로 범위/포인트 조회 가속
    - 트랜잭션(REDO-only WAL, BEGIN/COMMIT/ROLLBACK)
    - 간단한 옵티마이저(보조 인덱스 선택 vs 풀스캔 결정)

### 구현 기능 목록

- [x] 데이터 CRUD
   - [x] INSERT, SELECT, UPDATE, DELETE 지원
   - [x] PK 범위 스캔 지원(B+Tree 이용)
   - [x] 기본키 제약사항(중복 방지) 검증
- [x] 파일 입출력(영속성)
   - [x] 프로그램 재시작 후에도 데이터 유지 가능
- [x] B+Tree 자료구조
   - [x] B+Tree 기반 OrderedIndex 구현
   - [x] 포인트 조회, 범위 스캔 지원
   - [x] 리밸런싱 및 높이 유지
- [x] 인덱스
   - [x] 옵티마이저가 인덱스 스캔 vs 풀스캔 선택
   - [x] INSERT/UPDATE/DELETE 시 인덱스 업데이트
   - [x] 보조 인덱스 `name` 1개 운용
- [x] 트랜잭션
   - [x] BEGIN/COMMIT/ROLLBACK 지원
- [x] 간단한 옵티마이저
   - [x] 플랜 선택 기능(WHERE name = ? -> 보조 인덱스 경로 선택)

### 📦 프로그램 실행 예시

```sql
==== Mini DB Console ====
테이블: [users]

[0] 종료
[1] 전체 조회
[2] PK로 조회
[3] 레코드 추가
[4] 레코드 수정(Patch)
[5] 레코드 삭제
[6] 컬럼=값 검색(findAllBy)
[7] 저장
[8] PK 범위 조회
[9] Tx Begin
[10] Tx Commit
[11] Tx Rollback

선택 ▶ 2
PK(id) 입력 ▶ 1
------------------
id   | name | age
------------------
1    | Ash  | 24
------------------
rows: 1

선택 ▶ 3
id ▶ 1
name ▶ kiwi
age ▶ 19
[ERROR] PK 중복  ← (이제 종료되지 않고 계속 진행)

선택 ▶ 3
id ▶ 3
name ▶ Carol
age ▶ 27
추가 완료

선택 ▶ 1
------------------------
id   | name  | age
------------------------
1    | Ash   | 24
2    | Bob   | 28
3    | Carol | 27
------------------------
rows: 3

선택 ▶ 9
Transaction Begin.

선택 ▶ 4
PK(id) 입력 ▶ 3
수정 값 (예: name=Alice, age=24 ▶
age=28
수정 완료

선택 ▶ 10
COMMIT 완료

선택 ▶ 2
PK(id) 입력 ▶ 3
------------------
id   | name  | age
------------------
3    | Carol | 28
------------------
rows: 1

선택 ▶ 8
PK from ▶ 1
PK to   ▶ 3
... (범위 결과 출력)

선택 ▶ 0
종료합니다.
```

### **🎯** 프로그래밍 요구사항

- 표준 입력/출력을 사용해 CLI 인터페이스 제공한다.
- 예외 상황 시 에러 메시지를 출력하고 종료된다.([ERROR] …)
- 함수 길이, 클래스 책임을 작게 유지한다.

### 🔧 현재 아키텍처 요약

- Storage
  - database.db : Database 객체 직렬화 스냅샷
  - database.wal : REDO-only WAL(커밋 로그). 부팅 시 로그 반영하여 DB 복구
- 버전 관리(MVCC-lite)
  - VersionChain: PK 마다 커밋된 스냅샷을 시간순으로 보관
  - SELECT는 스냅샷 기준으로 버전 선택
- 인덱스
  - 주 인덱스: B+Tree<String, VersionChain> PK
  - 보조 인덱스(name): B+Tree<String, Set<PK>>
- 옵티마이저
  - 규칙 기반: `WHERE name=?` -> 보조 인덱스 스캔/ 그 외 Primary 인덱스 풀스캔
- 트랜잭션
  - BEGIN/COMMIT/ROLLBACK 제공
  - COMMIT 시 WAL append -> commit sequence 부여 -> VersionChain 반영 -> 보조 인덱스 유지
  - 격리 수준: READ COMMITTED(트랜잭션 내 재조회 시 외부 커밋 반영 가능)
