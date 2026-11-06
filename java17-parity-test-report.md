# Java 17 parity test run: staging-dvla

This is the Java 17 half of the CHD parity check. The `chd…` tests from the Java 25 branch
(`dev/25.104-chd-2875-j25-j17-parity`, see `java25-parity-test-report.md` there) were ported to `main` and run on JDK 17
to confirm the Java 17 behaviour each one asserts. After a review on 2026-11-06, only the tests that cover something
nothing else does were kept, on both branches.

## Run details

| | |
|---|---|
| Branch | `dev/chd-2875-parity-with-j25`, from `main` @ `2c6b4975` (not committed or pushed) |
| Location | `/Users/sbegarihmcts/HMCTS/repo/cpp-context-staging-dvla-j17` (a git worktree of the staging-dvla repo) |
| JDK | 17.0.10 (Oracle) |
| Maven | 3.9.6, run as `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home /opt/homebrew/Cellar/maven/3.9.6/libexec/bin/mvn` (Homebrew's `mvn` wrapper ignores `JAVA_HOME` and runs on JDK 25) |
| Latest result (2026-11-06, after the review) | `mvn -o -B install -Dmaven.test.failure.ignore=true` on every changed module: BUILD SUCCESS. **Only red test: CHD-01 (`NoResultException`), as expected.** The only skip is `MaterialAddedProcessorTest`, already `@Disabled` on `main`. |
| Before the review | 446 tests: 2 failures, 1 error, 1 skipped (CHD-01 and CHD-14 ×2, all expected) |
| Production code | Unchanged |
| Integration tests | Not run on this branch. The Java 25 branch ran 106 ITs with 0 failures and 0 errors. |

## Tests kept

| ID | Test (class · method) | Java 17 | Java 25 | Outcome |
|---|---|---|---|---|
| CHD-01 | `DriverAuditReportRepositoryTest` · `chd01_shouldReturnNullWhenReportBelongsToAnotherUserOrMaterial_acceptedJava25Contract` | ❌ `NoResultException` | ✅ | **Expected.** Pins the accepted J25 contract (null, which the caller turns into a 403). |
| CHD-02 | `DriverAuditRepositoryTest` · `chd02_shouldReadAuditDateTimeBackInUtcWhateverTheJvmZone` | ✅ | ✅ | **Parity.** J17's framework `EntityManagerProducer` sets the JVM default to UTC, so both JDKs read UTC; only the zone label differs (`UTC` vs `Z`). |
| CHD-03 | `QueryStagingdvlaApiAccessControlTest` · `chd03_shouldAllowDriverSearchByPermissionWhenUserIsInNoAllowedGroup`, `chd03_shouldNotAllowUnauthorisedUserToQueryDriverImage`, `chd03_shouldAllowAuditActionsForAuditors`, `chd03_shouldDenyAuditActionsForNonAuditors` | ✅ 4/4 | ✅ 4/4 | Parity |
| CHD-03 | `StagingdvlaCommandApiAccessControlTest` (new) · 4 `chd03_*` allow/deny tests | ✅ 4/4 | ✅ 4/4 | Parity |
| CHD-05 | `RestEasyClientServiceTest` · `chd05_shouldPostJsonWithApimHeadersOverHttpsToAServerWithAnUntrustedCertificateForAnotherHost` | ✅ | ✅ | Parity (RESTEasy 4 and RESTEasy 7) |
| CHD-09 | `DriverAuditRepositoryTest` · `chd09_shouldIncludeRowsExactlyOnTheStartAndEndBoundsAndExcludeOneMillisecondOutside` | ✅ | ✅ | Parity on H2. PostgreSQL still needs a check. |
| CHD-13 | `DriverAuditReportRepositoryTest` · `chd13_shouldUpdateTheExistingRowWhenSavingAnInstanceWithAnExistingId` | ✅ | ✅ | Parity |

## Tests removed in the review

Each was run on both JDKs before removal; the results are kept here for the record.

| ID | Test | Java 17 | Java 25 | Why removed |
|---|---|---|---|---|
| CHD-02 | `DriverAuditReportRepositoryTest` · `chd02_shouldReadSearchDateTimeBackInUtcWhateverTheJvmZone` | ✅ | ✅ | Same persistence-unit setting as the CHD-02 test kept. |
| CHD-03 | load-all-rules (query and command) | ✅ | ✅ | The allow tests already prove every rule loaded. |
| CHD-03 | deny-unknown-action (query and command) | ✅ | ✅ | Tests the Drools default, not our rules. |
| CHD-03 | `…DenyDriverSearchWithNeitherGroupNorPermission` | ✅ | ✅ | Cut down to the driver-image test above; the existing `shouldNotAllow…` tests cover the other two actions. |
| CHD-04 | `DriverNotificationHandlerTest` · `chd04_…` | ✅ | ✅ | Only recorded invalid IT test data, and read the integration-test module's files through a `../../` path. Fix the 3 payloads instead. |
| CHD-06 | `DriverNotificationHandlerTest` · `chd06_…` | ✅ | ✅ | One-off upgrade check; the ITs send the same payloads to the real handler. Same `../../` path problem. |
| CHD-07 | `FileServiceTest` · `chd07_…` (the class was new and held only this test, so it is gone) | ✅ | ✅ | Tests Tika and the framework, not our code. |
| CHD-08 | `DefaultQueryApi…ResourceTest` · `chd08_…` | ✅ | ✅ | Tests the REST generator's output. |
| CHD-10 | `StagingdvlaQueryApiTest` · `chd10_…` | ✅ | ✅ | Tests the framework `ObjectMapper`; `QueryDrivingLicencesIT` covers the audit path. |
| CHD-13 | `chd13_shouldMakeThePassedInstanceManagedWhenSavingANewReport` | ✅ | ❌ | Accepted divergence; no caller relies on it. |
| CHD-14 | `chd14_…RemovingADetachedReport` and `chd14_…RemovingNull` | ❌ `QueryInvocationException` | detached: ❌ (row deleted); null: ✅ | Accepted divergence. DeltaSpike wrapped the `IllegalArgumentException` on J17. The only caller removes an entity it has just loaded. |

## How the tests were adapted for `main`

Assertions are identical to the Java 25 versions; only the plumbing differs:

- `javax` instead of `jakarta`.
- The repository tests are JUnit 4 with DeltaSpike's test runner on OpenEJB, with `@Inject EntityManager`.
  `flushAndClear()` only calls `clear()`, because each repository call commits its own transaction. Rows commit, so the
  CHD tests use their own user IDs.
- `QueryStagingdvlaApiAccessControlTest`: `@Disabled` removed and it uses `QUERY_API_SESSION`, the same fix as on Java 25.
- `StagingdvlaCommandApiAccessControlTest`: copied over.
- `stagingdvla-event-processor/pom.xml`: two versionless test-scope dependencies the CHD-05 test needs to run the real
  RESTEasy 4 client outside WildFly: `org.slf4j:jcl-over-slf4j` and `javax.servlet:javax.servlet-api`.
- Each CHD test has a one-line comment.

## Next steps

1. Fix the 3 invalid IT payloads (CHD-04) on both branches, in a separate change.
2. This branch is evidence only. Decide whether to keep it (commit or push) or discard it after review.
