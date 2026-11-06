# staging-dvla: Java 25 parity test report

Branch `dev/25.104-chd-2875-j25-j17-parity` (base `cf6541a3`). Started 2026-11-05; updated 2026-11-06 with the JDK 17
run and the integration test run. The Java 17 half is in `java17-parity-test-report.md` on `dev/chd-2875-parity-with-j25`
(`/Users/sbegarihmcts/HMCTS/repo/cpp-context-staging-dvla-j17`).

This follows step 3 of the [Parity Testing Java17 → Java25](https://hmcts.atlassian.net/wiki/spaces/CTP/pages/279752455)
method. Each risk from the CHD risk register gets a unit test that asserts the **Java 17** behaviour. The test sits in
the existing test class for that code, and its method name starts with the risk ID. A red test on Java 25 is a finding.
Fix it in production, or accept it and log it here. Don't loosen the assertion.

**No production code was changed.** Test-only changes:

- `@Disabled` came off `QueryStagingdvlaApiAccessControlTest`; see CHD-03.
- New `StagingdvlaCommandApiAccessControlTest`. The command API had no access-control test class.
- A versionless test-scope `org.slf4j:jcl-over-slf4j` in `stagingdvla-event-processor/pom.xml` (BOM-managed, 2.0.17).
  It lets the CHD-05 test build the real RESTEasy client; WildFly supplies commons-logging at runtime.
- **Accepted divergences:** the two red tests are `@Disabled`, each with a reason and a comment explaining the acceptance.
  They are `chd13_shouldMakeThePassedInstanceManagedWhenSavingANewReport` and
  `chd14_shouldThrowIllegalArgumentExceptionWhenRemovingADetachedReport`. The upgrade skill's ground rules say not to
  commit `@Disabled`, so expect reviewers to ask; the alternative is to rewrite them to pin the accepted J25 behaviour,
  as CHD-01 does.
- CHD-02 tests corrected and renamed (see CHD-02); unused `java.time.ZoneId` imports removed.

## How it was run

**Unit tests and integration tests (latest, 2026-11-06)**

- JDK 25.0.5. `CPP_DOCKER_DIR=/Users/sbegarihmcts/HMCTS/repo/cpp-developers-docker ./runIntegrationTests.sh`
  (`cpp-developers-docker` on branch `java-25`). Exit code 0.
- **Unit tests:** BUILD SUCCESS. **466 tests, 0 failures, 0 errors, 3 skipped.**
  - The 3 skips: the two accepted `@Disabled` CHD tests above, and `MaterialAddedProcessorTest`, which was already
    `@Disabled` on `main`.
- **Deploy and healthcheck:** `stagingdvla-service-25.104.2-CHD-2875-SNAPSHOT.war` deployed, and the healthcheck returned
  pong for 1 out of 1 WARs.
- **Integration tests:** **106 run, 0 failures, 0 errors, 0 skipped** (`failsafe-summary.xml`).

**First unit test run (2026-11-05, before any acceptance)**

- `mvn -o -B clean install -Dmaven.test.failure.ignore=true`: 466 tests, 4 failures, 0 errors, 1 skipped.
  - The 4 failures were CHD-02 ×2, CHD-13 and CHD-14 (detached).
- Before this work: 440 run, 0 failures, 5 skipped. The difference is 26 new `chd…` tests, and the 4 access-control
  tests that now run instead of being skipped.

**Java 17 confirmation.** The tests were ported to `main` and run on JDK 17.0.10, with identical assertions. The Java 17
column below comes from that run (2026-11-06): 446 tests, 2 failures, 1 error, 1 skipped. All 3 red tests are expected
(CHD-01 and CHD-14 ×2); see `java17-parity-test-report.md`.

## Results

| ID | Severity | What could change (J17 → J25) | Test (class · method) | Java 17 result | Java 25 result | Disposition |
|---|---|---|---|---|---|---|
| CHD-01 | Medium → **Accepted** | Report download for a report that's missing or isn't the user's: J17 `NoResultException` (500) → J25 `null` → **403** | `DriverAuditReportRepositoryTest` · `chd01_shouldReturnNullWhenReportBelongsToAnotherUserOrMaterial_acceptedJava25Contract` (+ existing `StagingdvlaQueryViewTest.shouldThrowForbiddenRequestExceptionWhenDriverAuditReportNotFound`) | ❌ `NoResultException`, as expected | ✅ It pins the **accepted J25 contract**, not J17. | **Accepted** by the team (the caller turns null into 403). Log it in the PR; the RAML documents no 403. |
| CHD-02 | High → **None (parity)** | `ZonedDateTime` read from the viewstore (audit `date`/`time`, report-list `searchDateTime`, audit-report CSV `Date`/`Time`). The concern was that J17 read in the JVM zone, so BST times would move an hour. **The JDK 17 run disproved it:** on J17 the framework's `EntityManagerProducer.create()` (`persistence-deltaspike` 17.104.0) calls `TimeZone.setDefault(UTC)`, so Hibernate 5 always read in UTC (zone `UTC`). J25 (Hibernate 6, `hibernate.jdbc.time_zone=UTC`) reads UTC as zone `Z`. | `DriverAuditReportRepositoryTest` · `chd02_shouldReadSearchDateTimeBackInUtcWhateverTheJvmZone`; `DriverAuditRepositoryTest` · `chd02_shouldReadAuditDateTimeBackInUtcWhateverTheJvmZone` (renamed from `…BackInTheJvmDefaultZone`; the original Europe/London, 01:30 expectation was wrong) | ✅ both | ✅ both | **No user-visible change.** With the JVM default set to Europe/London, 00:30 UTC reads back as the same instant, zone normalising to `ZoneOffset.UTC`, local time 00:30. Only the zone label differs (`UTC` vs `Z`), and nothing prints it. Logged. |
| CHD-03 | Medium | Drools 7 → 10 allow/deny (BC-03 is refuted on the page, but its residual guard didn't exist here) and BC-20 (a kbase loading 0 rules makes deny tests pass vacuously) | `QueryStagingdvlaApiAccessControlTest` · `chd03_shouldLoadAllSixQueryRulesIntoTheQueryApiKbase`, `chd03_shouldAllowDriverSearchByPermissionWhenUserIsInNoAllowedGroup`, `chd03_shouldDenyDriverSearchWithNeitherGroupNorPermission`, `chd03_shouldAllowAuditActionsForAuditors`, `chd03_shouldDenyAuditActionsForNonAuditors`, `chd03_shouldDenyAnActionThatNoRuleNames`; `StagingdvlaCommandApiAccessControlTest` · `chd03_*` (6) | ✅ 12/12 | ✅ 12/12 (query class 10/10, command 6/6). Both kbases load all their rules (6 and 3). | **Parity.** Keep the tests. The query class had been `@Disabled` because it used `COMMAND_API_SESSION`, which doesn't exist in the query module; it now uses `QUERY_API_SESSION`, and the shared `@AfterEach` verify moved into the four original tests (same assertions). |
| CHD-04 | Medium → **Low** | Stricter schema validation (org.json + everit 1.6.0) newly rejecting borderline payloads. The J25 IT log has 60 rejections from 3 IT payloads. | `DriverNotificationHandlerTest` · `chd04_shouldRejectTheThreeAmendReshareItPayloadsAgainstTheHandlerSchema` | ✅ | ✅ All 3 are rejected. | **Parity; pre-existing invalid test data**, not BC-13. The payloads break schemas that are identical in J17 and J25: `courtApplication` has `additionalProperties: false` and no `applicationSubType`, and `judicialResultCategory` is `FINAL/INTERMEDIARY/ANCILLARY` while they send `Intermediary`. Fix the test data in a separate change on **both** branches: `applicationAmendReshare/scenario4a/command2.json` and `appealAmendReshare/cimd_3240_ac{1,2}/command2.json` expect **0** notifications, so those IT steps pass because of the schema rejection, not the rule they're named after. |
| CHD-05 | Medium-Low | RESTEasy 4 → 7 client: `disableTrustManager()` (no certificate trust and no hostname check), APIM headers, JSON body over HTTPS | `RestEasyClientServiceTest` · `chd05_shouldPostJsonWithApimHeadersOverHttpsToAServerWithAnUntrustedCertificateForAnotherHost` | ✅ | ✅ The real client POSTs over TLS to a local server with a self-signed certificate for `untrusted.example.invalid`; the body and the `Accept`, `Ocp-Apim-Subscription-Key` and `Ocp-Apim-Trace` headers arrive. | **Parity.** Still smoke-test the real DVLA API gateway (APIM) and material in the first deployed environment; connection pooling and the real certificate chain aren't covered. The query-side `RestEasyClientService` has no test class. |
| CHD-06 | Low-Medium | core-domain forked from a stale base (BC-15), so a valid payload could be rejected | `DriverNotificationHandlerTest` · `chd06_shouldAcceptEveryOtherDriverNotificationItPayloadAgainstTheHandlerSchema` | ✅ | ✅ All 226 other IT driver-notification payloads validate. | **Parity.** |
| CHD-07 | Low | Tika 1.28 → 3.3 MIME detection in the framework file store (`MetadataUpdater` → `ContentTypeDetector`) on the document payload `FileService` stores | `FileServiceTest` · `chd07_shouldStoreAPayloadThatTheFileStoreDetectsAsTextPlain` | ✅ | ✅ `text/plain` | **Parity.** |
| CHD-08 | Low | Codegen type scanning (reflections 0.9 → 0.10) producing a different set of REST resources | `DefaultQueryApiDriverAuditReportReportIdMaterialMaterialIdResourceTest` · `chd08_shouldHaveExactlyOneResourcePerRamlEndpointWithTheDownloadOneHandWritten` | ✅ | ✅ All 7 RAML resources have an interface and a `Default…Resource`; the download one is still the hand-written `@Stateless` class. | **Parity.** Separately, the hand-written resource bypasses the interceptor chain, so its Drools rule never runs. That's pre-existing; raise it with the team. Command-api and event adapters aren't covered. |
| CHD-09 | Low | pgjdbc / Hibernate 6 binding of `LocalDateTime` in the native audit date-range queries | `DriverAuditRepositoryTest` · `chd09_shouldIncludeRowsExactlyOnTheStartAndEndBoundsAndExcludeOneMillisecondOutside` | ✅ (H2) | ✅ (H2) | **Parity on H2.** The pgjdbc half needs a PostgreSQL run: an audit report whose range ends exactly on a record. The ITs ran on PostgreSQL and passed, but no IT targets that boundary. |
| CHD-10 | Low | Jackson 2.12 → 2.21 `ZonedDateTime` serialisation on the audit command (`StagingdvlaQueryApi:157`) | `StagingdvlaQueryApiTest` · `chd10_shouldSerialiseTheAuditDateTimeAsTheUtcInstantWithMillisecondsAndAZ` | ✅ | ✅ Europe/London 01:30:15.123456789 and the `UTC` region both give `2026-07-01T00:30:15.123Z`. | **Parity.** |
| CHD-11 | Low | `/internal/metrics` returns 404 (BC-16); `stream_error` hash and healthcheck message (BC-17) | **No test.** Framework-tier with no context code to test. | — | — | Confirm no dashboard or probe uses `/internal/metrics` for staging-dvla. (The local IT healthcheck still uses `/internal/metrics/ping`, and it passed.) |
| CHD-12 | Low (done) | Liquibase 5 rejects `liquibase.hub.mode` (BC-07) | **No test.** Deploy-time, and the fix is already applied. | — | — | Liquibase ran cleanly in the IT run. Watch the Helm pre-install Liquibase job on the first AKS deploy. |
| CHD-13 | Low → **Accepted** | DeltaSpike `save` called `persist` when the id had no row, which makes the passed instance managed; J25 always `merge`s and returns a managed copy | `DriverAuditReportRepositoryTest` · `chd13_shouldMakeThePassedInstanceManagedWhenSavingANewReport`; `chd13_shouldUpdateTheExistingRowWhenSavingAnInstanceWithAnExistingId` | ✅ both | ⏸ managed test **`@Disabled` (accepted)**; it was ❌ `Expected: is <true> but: was <false>`. ✅ update-existing-row. | **Accepted.** The row written is identical. Neither production caller (`DriverSearchAuditReportEventListener` lines 46 and 59) changes the entity after `save`. |
| CHD-14 | Low → **Accepted** | `remove` of a detached entity or `null`. **Corrected by the JDK 17 run:** J17 DeltaSpike wrapped the `IllegalArgumentException` in `QueryInvocationException`. | `DriverAuditReportRepositoryTest` · `chd14_shouldThrowIllegalArgumentExceptionWhenRemovingADetachedReport`; `chd14_shouldThrowIllegalArgumentExceptionWhenRemovingNull` | ❌ both: `QueryInvocationException`, not the bare `IllegalArgumentException` | ⏸ detached test **`@Disabled` (accepted)**; it was ❌ nothing thrown (J25 merges and deletes the row). ✅ `remove(null)` throws `IllegalArgumentException`. | **Accepted.** Detached: J17 refused (`QueryInvocationException`), J25 deletes. Null: both fail, only the exception class differs. The only caller (`handleDriverSearchAuditReportDeleted`) removes an entity it has just loaded in the same transaction. `shouldRemoveADetachedReportByMergingItFirst` pins the J25 contract. |

## Summary

- **Java 25: all green.**
  - Unit tests: 466 run, 0 failures, 0 errors, 3 skipped (2 accepted CHD tests, 1 pre-existing).
  - Integration tests: 106 run, 0 failures, 0 errors.
- **Parity confirmed on both JDKs** for CHD-02 to CHD-10, and the CHD-13 update test.
- **Accepted divergences:**
  - **CHD-01:** null, then 403, instead of a 500.
  - **CHD-13:** the passed instance is no longer managed after `save`.
  - **CHD-14:** a detached entity is deleted instead of rejected, and the null exception type changes.
  - No production caller is affected by CHD-13 or CHD-14.
- **CHD-02 was a false alarm.** Its Java 17 expectation was wrong: J17 also read these times back in UTC. The tests now
  assert UTC and pass on both JDKs. No user-visible change in any environment.
- **CHD-11 and CHD-12** have no context-level test, with the reasons given above.
- Pre-existing problems found along the way (the same on J17 and J25):
  - the 3 invalid IT payloads (CHD-04), whose IT steps pass for the wrong reason;
  - the hand-written download resource that bypasses access control (CHD-08);
  - the query access-control test that had been disabled since before the upgrade (CHD-03).

## Next steps

1. ~~Decide CHD-02.~~ Done: parity on both JDKs, tests corrected on both branches.
2. ~~Decide CHD-13 and CHD-14.~~ Done: accepted. The red tests are `@Disabled` with the reasons. Decide whether to keep
   `@Disabled` or rewrite them to pin the accepted J25 behaviour (as CHD-01 does) before raising a pull request.
3. ~~Port the `chd…` tests to `main` and run them on JDK 17.~~ Done; see `java17-parity-test-report.md`.
4. ~~Rerun the ITs.~~ Done: 106 run, 0 failures, 0 errors. Rerun them on the final commit.
5. Fix the 3 invalid IT payloads (CHD-04) on both branches, in a separate change.
6. Run a PostgreSQL check for the CHD-09 boundary, and smoke-test APIM and material (CHD-05) in the first deployed
   environment.
