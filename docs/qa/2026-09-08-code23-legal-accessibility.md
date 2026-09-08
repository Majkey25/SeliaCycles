# Code 23 legal links, accessibility and icon

Version `0.9.0-beta.15`. Checked September 8, 2026. This records the checks performed, not a legal or WCAG conformance certificate.

## Requested checklist

| Item | Result |
| --- | --- |
| Refund policy | Added to the EN/CS legal page. App is free with no Billing SDK or paid entitlement; voluntary external support is distinguished from app purchases. Mandatory rights are not waived. |
| Privacy policy | Existing page retained; added GitHub Pages hosting/IP-log disclosure and navigation to the other policies. |
| Terms of use | Added EN/CS terms describing free use, local profiles, export, licenses and medical limits. |
| Cookies policy / consent | Static site adds no scripts, cookies, storage identifiers or tracking. No unnecessary acceptance banner. External hosting/services are disclosed. |
| Form consent | No web forms or marketing signup exist. No form or checkbox was invented. In-app health details remain optional and local; calendar mirroring and file export require user actions. |
| Necessary data / tracking | No new data collection, permission, SDK, advertising or telemetry. Local profile and device-transfer behavior unchanged. |
| Colour contrast | Fixed faded adjacent-month text and matching-background custom entry markers. Existing readable-colour helper reused. Web normal text/link/meta combinations range from 5.86:1 to 16.41:1 across light/dark styles. |
| Alt text / labels | Website has no images. App decorative icons retain adjacent labels; calendar dates expose a labelled button instead of a duplicate bare numeral. Five About links have localized labels. |
| Keyboard / responsive web | Skip-to-content and refund-anchor navigation passed with keyboard. No horizontal overflow at 320, 768, 1024 or 1440 CSS pixels. Visible focus styles added; native links used. |
| Fake reviews / embeds | No testimonials or review widgets were found on the policy site. No iframe, embed, object, external font or tracking script added. |
| Image rights | Existing project ImageGen icon edited through ImageGen. No competitor code or asset copied. Prompt/provenance recorded; source/component licenses remain linked. No claim of exclusive rights to AI output. |
| Business details | Existing publisher label MajkeyLab, contact email and official repository linked. A registered legal name, business identifier and address were not supplied or independently verified; none was invented. The owner must confirm any legally required trader/controller details. |
| Unsupported claims | Pages explicitly exclude diagnosis, treatment, guaranteed fertility outcomes and contraception. No universal prediction-accuracy, legal-compliance or accessibility claim added. |

## Czech/EU reference check

- [ÚOOÚ cookies guidance](https://uoou.gov.cz/verejnost/qa-otazky-a-odpovedi/cookies): distinguish necessary operation from optional tracking and retain transparent information even without a banner.
- [GDPR processing principles](https://commission.europa.eu/law/law-topic/data-protection/information-business-and-organisations/principles-gdpr_en): data minimisation and purpose transparency.
- [ČOI digital-content guidance](https://coi.gov.cz/faq/smlouvy-o-poskytovani-digitalniho-obsahu-ci-sluzby/): mandatory consumer rights are not replaced by a blanket no-refund clause.
- [GitHub Pages hosting](https://docs.github.com/en/pages/getting-started-with-github-pages/what-is-github-pages): GitHub logs visitor IP addresses for security.

These checks do not resolve every jurisdiction's requirements, the publisher's legal status, health-data controller obligations, or a full transitive-license review. No new agreement was accepted and no personal details were guessed.

## Verification

- Baseline phone regression tests reproduced invisible custom-marker contrast of 1.0:1, adjacent-date contrast of 3.407:1, and a duplicate bare date numeral. An initial test fixture used an unsupported Wednesday week start; it was corrected to a supported Monday/Sunday value before those failures were measured.
- After the fixes, all 24 instrumentation tests passed together in 39.332 seconds on Huawei `BQLDU19927002646`, Android 10. Tests include date contrast at least 4.5:1, marker contrast at least 3:1, single date semantics, five outbound policy/repository URLs, period recalculation, import/export, profile isolation, editor safeguards and icon rendering.
- The legal-link test intercepts outbound browser intents; it checks destinations without sending health records or opening sign-in flows. The static pages were tested separately in a real browser.
- `testDebugUnitTest`: 156 tests. `lintDebug`: zero errors and eight existing warnings. Debug, QA, instrumentation, signed release APK and AAB builds passed.
- Native 48-pixel and 192-pixel icon renders were inspected. The red dot is bottom-right; the existing 12% adaptive inset is unchanged. The monochrome icon and feature graphic use the same position.
- Main phone app updated using `install -r`, without clearing its data. Code 23 and cold launch were verified. Single launch sample: 1,836 ms, not a performance benchmark.
- Only isolated QA packages were removed. Device released early around 08:43 with Home foreground; no system settings changed.

Manual end-to-end spoken TalkBack, every custom colour combination, and all assistive technologies were not tested. Website hosting and Play publication are verified separately after deployment.
