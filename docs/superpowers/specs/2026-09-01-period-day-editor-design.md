# Period day editor design

## Goal

Separate period-day editing from optional daily information so the two actions cannot overwrite each other.

## Interaction

- The day overview shows `Edit period` for today and past dates.
- `Edit period` opens a compact four-week calendar around the selected date or connected period.
- Tapping a date adds or removes that day from the period. Selected dates use the recorded-period color and a check state.
- The instruction sits directly below the date grid and uses an edit icon. The visible date range and selected-day count also have matching calendar and period icons.
- The editor accepts at most 14 days spanning at most 14 calendar days. Future dates are disabled.
- Saving an empty selection removes the connected period while preserving notes and other daily information.
- A separate `Add information` or `Edit information` action opens symptoms, mood, pain, notes, fertility signs and measurements.
- Daily information may edit flow intensity on an already recorded period day, but it cannot start or remove a period.

## Data flow

`PeriodActions.replace` owns changes to `bleeding` and `flow`. It updates only the selected connected period and newly selected days. Other periods and all optional fields remain unchanged. `MainViewModel.savePeriodDays` stores the changed logs and adjusts `activePeriodStart` only when the edited selection affects the active period.

The daily information sheet preserves the existing `bleeding` state. Deleting daily information clears optional fields but leaves the period day in place.

## Calendar marker

- Recorded and predicted period colors remain connected spans.
- The saved-prediction overlap underline is removed.
- The blue dot is replaced with a short blue underline at the bottom of the day cell.
- Bleeding alone never creates the underline. Only optional user or imported information does.
- The collapsed legend describes the blue underline, not a dot.

## Validation and accessibility

- Dates outside the supported model range and future dates cannot be selected.
- A selection larger than 14 days or spanning more than 14 calendar days is rejected with visible guidance.
- Every date toggle exposes its full localized date and selected state to accessibility services.
- Save, cancel, period editing and information editing use distinct labels and icons.
- New explanatory copy follows one visual language: place it next to the control it explains and pair it with a meaningful icon.

## Acceptance

1. Extend, shorten, split and completely remove a recorded period without losing notes or symptoms.
2. Add or delete daily information without changing whether the date is a period day.
3. Starting or editing a period still recalculates future period, fertile-window and ovulation estimates.
4. Calendar days with only bleeding have no marker. Days with optional information have a blue underline and no dot.
5. Unit tests, lint, release build and physical-device flows pass before code 17 is submitted to the closed Alpha track.
