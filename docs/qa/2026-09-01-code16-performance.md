# Code 16 calendar performance

Date: 2026-09-01

Device and gesture were held constant: Huawei YAL-L21, eight alternating horizontal month swipes, warm process, `dumpsys gfxinfo` reset immediately before the run.

| Metric | Code 15 baseline | Code 16 |
| --- | ---: | ---: |
| Frames | 326 | 322 |
| Janky frames | 17 (5.21%) | 13 (4.04%) |
| p50 | 6 ms | 5 ms |
| p90 | 14 ms | 10 ms |
| p95 | 16 ms | 14 ms |
| p99 | 31 ms | 23 ms |
| Slow UI-thread frames | 2 | 1 |
| Frame deadlines missed | 2 | 1 |
| Total PSS | 77,771 KiB | 76,036 KiB |

The calendar now memoizes derived date sets and formatters instead of rebuilding them per cell/recomposition. Calendar mirroring also compares provider fields and leaves unchanged events untouched. This is one controlled device run, not a fleet benchmark; the result supports a local improvement but does not prove identical behavior on every device.
