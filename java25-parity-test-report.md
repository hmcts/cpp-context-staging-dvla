# staging-dvla: Java 25 parity test report

Branch `dev/25.104-chd-2875-j25-j17-parity` (base `cf6541a3`). Started 2026-11-05; updated 2026-11-06 with the JDK 17
run, the integration test run and a review that removed the low-value tests. The Java 17 half is in
`java17-parity-test-report.md` on `dev/chd-2875-parity-with-j25`.

This follows step 3 of the [Parity Testing Java17 → Java25](https://hmcts.atlassian.net/wiki/spaces/CTP/pages/279752455)
method. Each CHD risk was checked with a unit test asserting the **Java 17** behaviour, run on both JDKs. A red test on
Java 25 is a finding: fix it in production, or accept it and log it here.

**No production code was changed.** Test-only changes kept on the branch:

- `@Disabled` came off `QueryStagingdvlaApiAccessControlTest`. It used `COMMAND_API_SESSION`, which the query module
  doesn't declare, so none of its rules had ever been tested. It now uses `QUERY_API_SESSION`.
- New `StagingdvlaCommandApiAccessControlTest`. The command API had no access-control test.
- A versionless test-scope `org.slf4j:jcl-over-slf4j` in `stagingdvla-event-processor/pom.xml` (BOM-managed). It lets
  the CHD-05 test build the real RESTEasy client; WildFly supplies commons-logging at runtime.
- 13 `chd…` tests, each with a one-line comment.

## Test review (2026-11-06)

Every CHD test was checked against the existing unit tests and the ITs. Only tests that cover something nothing else
does were kept.

| Removed | Why |
|---|---|
| CHD-13 managed-instance and CHD-14 detached-remove tests | Accepted divergences; they were `@Disabled` and added nothing. `shouldRemoveADetachedReportByMergingItFirst` pins the J25 detached behaviour. |
| CHD-02 on `DriverAuditReportRepositoryTest` | Same persistence-unit setting as the CHD-02 test kept on `DriverAuditRepositoryTest`. |
| CHD-03 load-all-rules (query and command) | The allow tests already prove every rule loaded; an allow can't pass without its rule. |
| CHD-03 deny-unknown-action (query and command) | Tests the Drools default, not our rules. |
| CHD-04 and CHD-06 schema checks of the IT payloads | CHD-04 only recorded invalid test data (fix the payloads instead). CHD-06 was a one-off upgrade check; the ITs send the same payloads to the real handler. |
| CHD-07 Tika `text/plain` | Tests Tika and the framework `ContentTypeDetector`; nothing in this context reads the media type. |
| CHD-08 one resource per RAML endpoint | Tests the REST generator; a missing resource fails the deploy or the ITs. |
| CHD-10 audit date-time serialisation | Tests the framework `ObjectMapper`; `QueryDrivingLicencesIT` covers the audit path end to end. |
| CHD-14 `remove(null)` | JPA behaviour with no logic of ours; the exception-type change is accepted. |

CHD-03 `…DenyDriverSearchWithNeitherGroupNorPermission` was cut down to
`chd03_shouldNotAllowUnauthorisedUserToQueryDriverImage`, because the existing `shouldNotAllow…` tests already cover the
other two actions.

## Results

The Java 17 column is from the JDK 17.0.10 run of the same tests on `main` (2026-11-06). The removed tests are listed
for the record with their result before removal.

| ID | What could change (J17 → J25) | Test kept | Java 17 | Java 25 | Disposition |
|---|---|---|---|---|---|
| CHD-01 | Report download for a report that isn't the user's: J17 `NoResultException` (500) → J25 `null` → **403** | `DriverAuditReportRepositoryTest` · `chd01_shouldReturnNullWhenReportBelongsToAnotherUserOrMaterial_acceptedJava25Contract` | ❌ `NoResultException`, as expected | ✅ pins the accepted J25 contract | **Accepted.** Also proves the user and material filters apply, which the existing random-id test does not. |
| CHD-02 | `ZonedDateTime` read back from the viewstore in a different zone | `DriverAuditRepositoryTest` · `chd02_shouldReadAuditDateTimeBackInUtcWhateverTheJvmZone` | ✅ | ✅ | **Parity.** J17's framework `EntityManagerProducer` sets the JVM default to UTC, so both read UTC; only the zone label differs (`UTC` vs `Z`), and nothing prints it. |
| CHD-03 | Drools 7 → 10 allow/deny | `QueryStagingdvlaApiAccessControlTest` · `chd03_shouldAllowDriverSearchByPermissionWhenUserIsInNoAllowedGroup`, `chd03_shouldNotAllowUnauthorisedUserToQueryDriverImage`, `chd03_shouldAllowAuditActionsForAuditors`, `chd03_shouldDenyAuditActionsForNonAuditors`; `StagingdvlaCommandApiAccessControlTest` · 4 `chd03_*` allow/deny tests | ✅ | ✅ | **Parity.** |
| CHD-04 | Stricter schema validation rejecting payloads | Removed | ✅ | ✅ | **Parity; pre-existing invalid test data.** `applicationAmendReshare/scenario4a/command2.json` and `appealAmendReshare/cimd_3240_ac{1,2}/command2.json` break schemas identical on J17 and J25, and their IT steps expect 0 notifications, so they pass for the wrong reason. Fix the data separately on both branches. |
| CHD-05 | RESTEasy 4 → 7 client over HTTPS with trust and hostname checks off | `RestEasyClientServiceTest` · `chd05_shouldPostJsonWithApimHeadersOverHttpsToAServerWithAnUntrustedCertificateForAnotherHost` | ✅ | ✅ | **Parity.** The only real TLS test; the ITs use plain-HTTP WireMock. Still smoke-test APIM in the first deployed environment. |
| CHD-06 | core-domain from a stale base rejecting valid payloads | Removed | ✅ | ✅ all 226 | **Parity.** |
| CHD-07 | Tika 1.28 → 3.3 MIME detection | Removed | ✅ | ✅ | **Parity.** |
| CHD-08 | Codegen producing a different set of REST resources | Removed | ✅ | ✅ | **Parity.** Pre-existing: the hand-written download resource bypasses the interceptor chain, so its Drools rule never runs. Raise it with the team. |
| CHD-09 | `LocalDateTime` binding in the native audit date-range queries | `DriverAuditRepositoryTest` · `chd09_shouldIncludeRowsExactlyOnTheStartAndEndBoundsAndExcludeOneMillisecondOutside` | ✅ (H2) | ✅ (H2) | **Parity on H2.** The existing range test doesn't hit the exact start or the ±1 ms edges. The PostgreSQL side still needs a check. |
| CHD-10 | Jackson `ZonedDateTime` serialisation on the audit command | Removed | ✅ | ✅ | **Parity.** |
| CHD-11 | `/internal/metrics` 404, healthcheck message | No test (framework) | — | — | Confirm no dashboard or probe uses `/internal/metrics`. |
| CHD-12 | Liquibase 5 rejects `liquibase.hub.mode` | No test (deploy time, fixed) | — | — | Liquibase ran cleanly in the IT run. |
| CHD-13 | `save` no longer makes the passed instance managed | `DriverAuditReportRepositoryTest` · `chd13_shouldUpdateTheExistingRowWhenSavingAnInstanceWithAnExistingId` | ✅ | ✅ | **Accepted** (managed instance). The kept test covers the update path the listener uses when a report is stored. |
| CHD-14 | `remove` of a detached entity or `null` | Removed | ❌ `QueryInvocationException` | detached deleted; null `IllegalArgumentException` | **Accepted.** The only caller removes an entity it has just loaded in the same transaction. |

## Runs

- **Latest full run (before the review), JDK 25.0.5:** `CPP_DOCKER_DIR=/Users/sbegarihmcts/HMCTS/repo/cpp-developers-docker ./runIntegrationTests.sh`,
  exit 0. Unit tests 466 run, 0 failures, 0 errors, 3 skipped. WAR deployed, healthcheck passed. ITs 106 run,
  0 failures, 0 errors.
- **After the review, JDK 25.0.5:** `mvn -o -B install` on every changed module (command-api, command-handler,
  viewstore-persistence, event-processor, query-api, integration-test): BUILD SUCCESS, 0 failures, 0 errors. The only
  skip is `MaterialAddedProcessorTest`, already `@Disabled` on `main`. Expected full counts on the next run: 453 unit
  tests, 1 skipped; 106 ITs.
- **JDK 17.0.10:** 446 tests, 2 failures, 1 error, 1 skipped; the 3 red tests were CHD-01 and CHD-14 ×2, all expected.
  See `java17-parity-test-report.md`.

## Next steps

1. Rerun `./runIntegrationTests.sh` on the final commit.
2. Apply the same test review to the Java 17 branch if it is kept.
3. Fix the 3 invalid IT payloads (CHD-04) on both branches, in a separate change.
4. PostgreSQL check for the CHD-09 boundary; smoke-test APIM and material (CHD-05) in the first deployed environment.
