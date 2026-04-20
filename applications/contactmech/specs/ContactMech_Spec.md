# ContactMech Product Specification

## 1. Overview

A **Contact Mechanism** (ContactMech) is a record that describes one way to reach a party (person or organisation). It can represent an email address, a postal address, a telephone number, an FTP server endpoint, a web address, an IP address, an electronic address, or a domain name.

Every ContactMech belongs to exactly one type (its **ContactMechType**). Depending on the type, the address data is stored either directly on the ContactMech record (as a free-text `infoString` — used for email, web, and similar types) or in a type-specific sub-entity record (PostalAddress, TelecomNumber, FtpAddress).

Updates follow an **immutability-with-history** pattern: when the address data changes, the old record is preserved and a new ContactMech (with a new identifier) is created to carry the updated data. The caller receives both the new and the old identifier so that any party associations can be updated accordingly.

---

## 2. Services

### 2.1 `createContactMech`

**Description:** Creates a new ContactMech record of the specified type. This is the foundational creation service that all type-specific create services delegate to.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechTypeId | String (id) | Yes | The type of contact mechanism to create. |
| contactMechId | String (id) | No | Caller-supplied identifier. If omitted, the system generates one. |
| infoString | String (long-varchar) | No | Free-text value for string-based types (e.g. email address, URL). |

**Outputs:**

| Field | Type | Description |
|---|---|---|
| contactMechId | String (id) | The identifier of the newly created ContactMech record. |

**Business Rules:**

1. If `contactMechId` is not supplied, the system auto-generates a unique identifier.
2. `contactMechTypeId` must refer to an existing ContactMechType record.

**Error Cases:**

- Framework-level error if `contactMechTypeId` is absent (required field missing).

**Side Effects:**

- One new `ContactMech` record is persisted.

---

### 2.2 `updateContactMech`

**Description:** Updates a generic ContactMech record. If the `infoString` value has actually changed, the old record is left in place and a new ContactMech record is created. If the value is unchanged, the call succeeds without creating any new record.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechId | String (id) | Yes | Identifier of the ContactMech to update. |
| contactMechTypeId | String (id) | No | Type of the contact mechanism. Used to select the localised success message. |
| infoString | String (long-varchar) | No | New free-text value. |

**Outputs:**

| Field | Type | Description |
|---|---|---|
| contactMechId | String (id) | Identifier of the resulting ContactMech record. Equals the input `contactMechId` when no change occurred; otherwise is the newly created record's identifier. |

**Business Rules:**

1. The ContactMech record identified by the input `contactMechId` must exist.
2. If `infoString` in the stored record differs from the supplied `infoString`, a new ContactMech record is created (via `createContactMech`) with the updated data and the new identifier is returned.
3. If `infoString` is unchanged, no new record is created and the original `contactMechId` is returned.

**Error Cases:**

| Condition | Message key | Message source |
|---|---|---|
| `contactMechId` not found | `ServiceValueNotFound` | `ServiceErrorUiLabels` |

**Side Effects:**

- When the value changes: one new `ContactMech` record is persisted. The original record is not modified or deleted.
- When unchanged: no storage changes.

---

### 2.3 `createPostalAddress`

**Description:** Creates a postal address. A ContactMech record of type `POSTAL_ADDRESS` and a corresponding PostalAddress sub-entity record are both persisted.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| address1 | String (long-varchar) | Yes | Primary street line. |
| city | String (name) | Yes | City name. |
| postalCode | String (short-varchar) | Yes | Postal or ZIP code. |
| contactMechId | String (id) | No | Caller-supplied identifier; auto-generated if omitted. |
| toName | String (name) | No | Recipient name. |
| attnName | String (name) | No | Attention-to name. |
| address2 | String (long-varchar) | No | Secondary street line. |
| houseNumber | Numeric | No | House number. |
| houseNumberExt | String (short-varchar) | No | House number extension. |
| directions | String (long-varchar) | No | Navigation directions to the address. |
| cityGeoId | String (id) | No | Geographic identifier for the city. |
| postalCodeExt | String (short-varchar) | No | Postal code extension. |
| countryGeoId | String (id) | No | Geographic identifier for the country. |
| stateProvinceGeoId | String (id) | No | Geographic identifier for the state or province. Conditionally required (see Business Rules). |
| countyGeoId | String (id) | No | Geographic identifier for the county. |
| municipalityGeoId | String (id) | No | Geographic identifier for the municipality. |
| postalCodeGeoId | String (id) | No | Geographic identifier for the postal code area. |
| geoPointId | String (id) | No | Reference to a geographic point (coordinates). |

**Outputs:**

| Field | Type | Description |
|---|---|---|
| contactMechId | String (id) | Identifier of the newly created ContactMech/PostalAddress. |

**Business Rules:**

1. When `countryGeoId` is `USA`, `stateProvinceGeoId` must be provided.
2. When `countryGeoId` is `CAN`, `stateProvinceGeoId` must be provided.
3. For all other countries, `stateProvinceGeoId` is optional.

**Error Cases:**

| Condition | Message key | Message source |
|---|---|---|
| USA address missing state | `PartyStateInUsMissing` | `PartyUiLabels` |
| Canada address missing province | `PartyProvinceInCanadaMissing` | `PartyUiLabels` |
| `address1` not supplied | Framework required-field error | — |
| `city` not supplied | Framework required-field error | — |
| `postalCode` not supplied | Framework required-field error | — |

**Side Effects:**

- One new `ContactMech` record with `contactMechTypeId = POSTAL_ADDRESS`.
- One new `PostalAddress` record keyed on the same `contactMechId`.

---

### 2.4 `updatePostalAddress`

**Description:** Updates an existing postal address. Follows the immutability-with-history pattern: if any PostalAddress field has changed, a new PostalAddress (and its backing ContactMech) is created. If the PostalAddress data is unchanged but the ContactMech itself changed (see `updateContactMech`), only a new PostalAddress record is created to point to the new ContactMech. If nothing changed, the call succeeds without any new records.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechId | String (id) | Yes (INOUT) | Identifier of the existing ContactMech/PostalAddress to update. On output, holds the identifier of the resulting ContactMech. |
| address1 | String (long-varchar) | Yes | Primary street line. |
| city | String (name) | Yes | City name. |
| postalCode | String (short-varchar) | Yes | Postal or ZIP code. |
| toName | String (name) | No | Recipient name. |
| attnName | String (name) | No | Attention-to name. |
| address2 | String (long-varchar) | No | Secondary street line. |
| houseNumber | Numeric | No | House number. |
| houseNumberExt | String (short-varchar) | No | House number extension. |
| directions | String (long-varchar) | No | Navigation directions. |
| cityGeoId | String (id) | No | City geo identifier. |
| postalCodeExt | String (short-varchar) | No | Postal code extension. |
| countryGeoId | String (id) | No | Country geo identifier. |
| stateProvinceGeoId | String (id) | No | State/province geo identifier. Conditionally required (see Business Rules). |
| countyGeoId | String (id) | No | County geo identifier. |
| municipalityGeoId | String (id) | No | Municipality geo identifier. |
| postalCodeGeoId | String (id) | No | Postal code geo identifier. |
| geoPointId | String (id) | No | Geo point reference. |
| partyId | String (id) | No | Party identifier (passed through; not used directly by this service). |
| latitude | String | No | Latitude (passed through; not used directly by this service). |
| longitude | String | No | Longitude (passed through; not used directly by this service). |

**Outputs:**

| Field | Type | Description |
|---|---|---|
| contactMechId | String (id) | Identifier of the resulting ContactMech. New value when data changed; same as input when nothing changed. |
| oldContactMechId | String (id) | Identifier of the ContactMech that was current before this call. Always set. |

**Business Rules:**

1. When `countryGeoId` is `USA`, `stateProvinceGeoId` must be provided.
2. When `countryGeoId` is `CAN`, `stateProvinceGeoId` must be provided.
3. The PostalAddress record for the given `contactMechId` must exist.
4. If the new PostalAddress field values differ from the stored values, a new ContactMech + PostalAddress pair is created (via `createPostalAddress`) and the new `contactMechId` is returned.
5. If the PostalAddress field values are unchanged but `updateContactMech` returns a different `contactMechId` (because `infoString` changed), a new PostalAddress record is created against the new `contactMechId`.
6. If nothing changed, the original `contactMechId` is returned unchanged (no new records created).

**Error Cases:**

| Condition | Message key / description | Message source |
|---|---|---|
| USA address missing state | `PartyStateInUsMissing` | `PartyUiLabels` |
| Canada address missing province | `PartyProvinceInCanadaMissing` | `PartyUiLabels` |
| PostalAddress record not found | `ServiceValueNotFound` | `ServiceErrorUiLabels` |
| `address1` not supplied | Framework required-field error | — |
| `city` not supplied | Framework required-field error | — |
| `postalCode` not supplied | Framework required-field error | — |

**Side Effects:**

- When PostalAddress data changed: one new `ContactMech` (type `POSTAL_ADDRESS`) and one new `PostalAddress` record. Originals are preserved.
- When only ContactMech changed: one new `PostalAddress` record linked to the new `contactMechId`. Original PostalAddress is preserved.
- When nothing changed: no storage changes.

---

### 2.5 `createTelecomNumber`

**Description:** Creates a telecommunications number. A ContactMech record of type `TELECOM_NUMBER` and a corresponding TelecomNumber sub-entity record are both persisted.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechId | String (id) | No | Caller-supplied identifier; auto-generated if omitted. |
| countryCode | String (very-short) | No | International dialling country code (e.g. `1`, `44`). |
| areaCode | String (very-short) | No | Area or city code. |
| contactNumber | String (short-varchar) | No | The local subscriber number. |
| askForName | String (name) | No | Person or department to ask for when calling. |

**Outputs:**

| Field | Type | Description |
|---|---|---|
| contactMechId | String (id) | Identifier of the newly created ContactMech/TelecomNumber. |

**Business Rules:**

1. All TelecomNumber fields are optional at the service level; the system accepts any combination, including a `contactNumber`-only record.

**Error Cases:**

- Framework-level errors only (no service-level validation beyond required fields).

**Side Effects:**

- One new `ContactMech` record with `contactMechTypeId = TELECOM_NUMBER`.
- One new `TelecomNumber` record keyed on the same `contactMechId`.

---

### 2.6 `updateTelecomNumber`

**Description:** Updates an existing telecom number. Follows the immutability-with-history pattern: if any TelecomNumber field has changed, a new TelecomNumber (and its backing ContactMech) is created. If the data is unchanged but the ContactMech itself changed, only a new TelecomNumber record is created.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechId | String (id) | Yes (INOUT) | Identifier of the existing ContactMech/TelecomNumber to update. |
| countryCode | String (very-short) | No | International dialling country code. |
| areaCode | String (very-short) | No | Area or city code. |
| contactNumber | String (short-varchar) | No | Local subscriber number. |
| askForName | String (name) | No | Person or department to ask for. |

**Outputs:**

| Field | Type | Description |
|---|---|---|
| contactMechId | String (id) | Identifier of the resulting ContactMech. New value when data changed; same as input otherwise. |
| oldContactMechId | String (id) | Identifier of the ContactMech that was current before this call. |

**Business Rules:**

1. The TelecomNumber record for the given `contactMechId` must exist.
2. If any TelecomNumber field value differs from the stored value, a new ContactMech + TelecomNumber pair is created (via `createTelecomNumber`) and the new `contactMechId` is returned.
3. If TelecomNumber data is unchanged but `updateContactMech` returns a different `contactMechId`, a new TelecomNumber record is created against the new `contactMechId`.
4. If nothing changed, the original `contactMechId` is returned unchanged.

**Error Cases:**

| Condition | Message key | Message source |
|---|---|---|
| TelecomNumber record not found | `ServiceValueNotFound` | `ServiceErrorUiLabels` |

**Side Effects:**

- When data changed: one new `ContactMech` (type `TELECOM_NUMBER`) and one new `TelecomNumber` record. Originals are preserved.
- When only ContactMech changed: one new `TelecomNumber` record. Original preserved.
- When nothing changed: no storage changes.

---

### 2.7 `createEmailAddress`

**Description:** Creates a ContactMech record for an email address after validating the email format. The email address is stored in the `infoString` field of the ContactMech record; no sub-entity record is created.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| emailAddress | String | Yes | The email address to store. Must be a valid RFC-compliant email address. |
| contactMechId | String (id) | No | Caller-supplied identifier; auto-generated if omitted. |

**Outputs:**

| Field | Type | Description |
|---|---|---|
| contactMechId | String (id) | Identifier of the newly created ContactMech. |

**Business Rules:**

1. `emailAddress` must pass email-format validation (`UtilValidate.isEmail`). Addresses without an `@` sign, or other malformed values, are rejected.
2. The email address is stored verbatim in `ContactMech.infoString`.
3. No separate sub-entity record (beyond `ContactMech`) is created for email addresses.

**Error Cases:**

| Condition | Message key | Message source |
|---|---|---|
| `emailAddress` fails format validation | `PartyEmailAddressNotFormattedCorrectly` | `PartyUiLabels` |

**Side Effects:**

- One new `ContactMech` record with `contactMechTypeId = EMAIL_ADDRESS` and `infoString` set to the supplied email address.

---

### 2.8 `updateEmailAddress`

**Description:** Updates an existing email-address ContactMech. Validates format before delegating to `updateContactMech`. The immutability-with-history rule applies: if the address changed, a new ContactMech record is created.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechId | String (id) | Yes (INOUT) | Identifier of the existing email-address ContactMech to update. |
| emailAddress | String | Yes | The new email address. Must be a valid RFC-compliant email address. |

**Outputs:**

| Field | Type | Description |
|---|---|---|
| contactMechId | String (id) | Identifier of the resulting ContactMech. New value when the address changed; same as input otherwise. |

**Business Rules:**

1. `emailAddress` must pass email-format validation before any storage is touched.
2. Delegates to `updateContactMech` with `contactMechTypeId = EMAIL_ADDRESS` and `infoString = emailAddress`.

**Error Cases:**

| Condition | Message key | Message source |
|---|---|---|
| `emailAddress` fails format validation | `PartyEmailAddressNotFormattedCorrectly` | `PartyUiLabels` |

**Side Effects:**

- Inherits the side effects of `updateContactMech`: either one new `ContactMech` record (when the address changed) or no storage changes.

---

### 2.9 `createFtpAddress`

**Description:** Creates an FTP server address. A ContactMech record of type `FTP_ADDRESS` and a corresponding FtpAddress sub-entity record are both persisted.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| hostname | String (long-varchar) | No | FTP server hostname or URL. |
| port | Numeric | No | Port number. |
| username | String (long-varchar) | No | FTP login username. |
| ftpPassword | String (long-varchar, encrypted) | No | FTP login password. Stored encrypted. |
| binaryTransfer | Indicator (Y/N) | No | Whether to use binary transfer mode. |
| filePath | String (long-varchar) | No | Default remote file path. |
| zipFile | Indicator (Y/N) | No | Whether to zip files before transfer. |
| passiveMode | Indicator (Y/N) | No | Whether to use passive FTP mode. |
| defaultTimeout | Numeric | No | Connection timeout in milliseconds. |

**Outputs:**

| Field | Type | Description |
|---|---|---|
| contactMechId | String (id) | Identifier of the newly created ContactMech/FtpAddress. |

**Business Rules:**

1. All FtpAddress fields are optional; a minimal call (no fields) creates a valid empty record.
2. Requires the caller to have the `partyBasePermissionCheck` CREATE permission.
3. If the underlying `createContactMech` call does not return a `contactMechId`, creation is aborted and an error is returned.

**Error Cases:**

| Condition | Error message |
|---|---|
| `createContactMech` returns no `contactMechId` | `'Error creating contactMech'` (literal string) |

**Side Effects:**

- One new `ContactMech` record with `contactMechTypeId = FTP_ADDRESS`.
- One new `FtpAddress` record keyed on the same `contactMechId`.

---

### 2.10 `updateFtpAddressWithHistory`

**Description:** Updates an existing FTP address. Follows the immutability-with-history pattern: if any FtpAddress field has changed, a new FtpAddress (and its backing ContactMech) is created. If unchanged, only the ContactMech level is updated (via `updateContactMech`). Always returns both the resulting and the original `contactMechId`.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechId | String (id) | Yes (INOUT) | Identifier of the existing ContactMech/FtpAddress. |
| hostname | String (long-varchar) | No | Updated hostname. |
| port | Numeric | No | Updated port. |
| username | String (long-varchar) | No | Updated username. |
| ftpPassword | String (long-varchar, encrypted) | No | Updated password. |
| binaryTransfer | Indicator (Y/N) | No | Updated binary-transfer flag. |
| filePath | String (long-varchar) | No | Updated file path. |
| zipFile | Indicator (Y/N) | No | Updated zip-file flag. |
| passiveMode | Indicator (Y/N) | No | Updated passive-mode flag. |
| defaultTimeout | Numeric | No | Updated timeout. |

**Outputs:**

| Field | Type | Description |
|---|---|---|
| contactMechId | String (id) | Identifier of the resulting ContactMech. New value when data changed; same as input otherwise. |
| oldContactMechId | String (id) | Identifier of the ContactMech that was current before this call. |

**Business Rules:**

1. Requires the caller to have the `partyBasePermissionCheck` UPDATE permission.
2. If `contactMechId` is not provided, no update is performed and the service returns success with both output fields unset.
3. If FtpAddress data has changed, a new FtpAddress + ContactMech is created (via `createFtpAddress`).
4. If FtpAddress data is unchanged, only `updateContactMech` is called (with `contactMechTypeId = FTP_ADDRESS`).

**Error Cases:**

- None defined beyond permission failure.

**Side Effects:**

- When data changed: one new `ContactMech` (type `FTP_ADDRESS`) and one new `FtpAddress`. Originals preserved.
- When unchanged and ContactMech changed: same as `updateContactMech` side effects.
- When nothing changed: no storage changes.

---

### 2.11 `createPartyFtpAddress`

**Description:** Creates an FTP address and links it to a party in a single operation. Internally calls `createFtpAddress` followed by `createPartyContactMech`.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| partyId | String (id) | Yes | The party to associate the FTP address with. |
| hostname | String (long-varchar) | No | FTP server hostname. |
| port | Numeric | No | Port number. |
| username | String (long-varchar) | No | FTP login username. |
| ftpPassword | String (long-varchar) | No | FTP login password. |
| binaryTransfer | Indicator (Y/N) | No | Binary transfer flag. |
| filePath | String (long-varchar) | No | Default file path. |
| zipFile | Indicator (Y/N) | No | Zip flag. |
| passiveMode | Indicator (Y/N) | No | Passive-mode flag. |
| defaultTimeout | Numeric | No | Connection timeout. |

**Outputs:**

| Field | Type | Description |
|---|---|---|
| contactMechId | String (id) | Identifier of the newly created ContactMech/FtpAddress. |

**Business Rules:**

1. If `createFtpAddress` returns an error, that error is propagated immediately and no party link is created.
2. If `createPartyContactMech` (external service) returns an error, that error is propagated.
3. Purpose assignment is not currently implemented (noted as a TODO).

**Error Cases:**

- Propagates any error from `createFtpAddress` or `createPartyContactMech`.

**Side Effects:**

- All side effects of `createFtpAddress`.
- One new `PartyContactMech` record linking `partyId` to the new `contactMechId` (via external service `createPartyContactMech`).

---

### 2.12 `updatePartyFtpAddress`

**Description:** Updates an FTP address that is already linked to a party. If the FTP data changed and a new ContactMech is created, the party's association is re-pointed to the new ContactMech.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| partyId | String (id) | Yes | Party whose FTP address is being updated. |
| contactMechId | String (id) | Yes | Current ContactMech identifier for the FTP address. |
| hostname | String (long-varchar) | No | Updated hostname. |
| port | Numeric | No | Updated port. |
| username | String (long-varchar) | No | Updated username. |
| ftpPassword | String (long-varchar) | No | Updated password. |
| binaryTransfer | Indicator (Y/N) | No | Updated binary-transfer flag. |
| filePath | String (long-varchar) | No | Updated file path. |
| zipFile | Indicator (Y/N) | No | Updated zip flag. |
| passiveMode | Indicator (Y/N) | No | Updated passive-mode flag. |
| defaultTimeout | Numeric | No | Updated timeout. |

**Outputs:**

| Field | Type | Description |
|---|---|---|
| contactMechId | String (id) | Identifier of the resulting ContactMech. New value when FTP data changed; same as input otherwise. |

**Business Rules:**

1. Calls `updateFtpAddressWithHistory`; if the result contains a new `contactMechId`, calls external service `updatePartyContactMech` to re-point the party association.

**Error Cases:**

- None defined beyond those propagated from `updateFtpAddressWithHistory` or `updatePartyContactMech`.

**Side Effects:**

- All side effects of `updateFtpAddressWithHistory`.
- When the ContactMech changed: the `PartyContactMech` record is updated to point to the new `contactMechId` (via external service `updatePartyContactMech`).

---

### 2.13 `sendVerifyEmailAddressNotification`

**Description:** Sends a verification email to an address when a matching `EmailAddressVerification` record and a store-level email template both exist. If either is absent the service returns success silently without sending any email.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| emailAddress | String | Yes | The email address to send the verification to. |
| verifyHash | String | No | The verification hash; looked up from `EmailAddressVerification` by the `emailAddress` key. |

**Outputs:**

_(None beyond the standard success/error response.)_

**Business Rules:**

1. The service only sends the email when **both** conditions are true: an `EmailAddressVerification` record exists for the given `emailAddress`, **and** a `ProductStoreEmailSetting` record of type `PRDS_EMAIL_VERIFY` exists.
2. If either record is absent, the service returns success without sending.
3. The `webSiteId` in the email parameters is set from the first `WebSite` associated with the product store; if none is found it is omitted.

**Error Cases:**

- None; the service always returns success.

**Side Effects:**

- When both prerequisite records are found: an email is dispatched via external service `sendMailFromScreen`.

---

### 2.14 `verifyEmailAddress`

**Description:** Validates an email-address verification token. Confirms the token exists and has not expired.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| verifyHash | String | Yes | The verification token to validate. |

**Outputs:**

_(None beyond the standard success/error response.)_

**Business Rules:**

1. An `EmailAddressVerification` record with the given `verifyHash` must exist.
2. The record's `expireDate` must not be in the past relative to the current date/time.

**Error Cases:**

| Condition | Message key | Message source |
|---|---|---|
| No `EmailAddressVerification` found for `verifyHash` | `PartyEmailAddressNotExist` | `PartyUiLabels` |
| Verification record has expired (`expireDate` is before now) | `PartyEmailAddressVerificationExpired` | `PartyUiLabels` |

**Side Effects:**

- None; this is a read-only validation service.

---

### 2.15 `createContactMechType`

**Description:** Creates a new ContactMechType record defining a category of contact mechanism.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechTypeId | String (id) | No | Caller-supplied identifier; auto-generated if omitted. |
| parentTypeId | String (id) | No | Parent type for hierarchical classification. |
| hasTable | Indicator (Y/N) | No | Whether this type has a dedicated sub-entity table. |
| description | String (description) | No | Human-readable description. |

**Outputs:**

| Field | Type | Description |
|---|---|---|
| contactMechTypeId | String (id) | Identifier of the newly created type. |

**Side Effects:** One new `ContactMechType` record.

---

### 2.16 `updateContactMechType`

**Description:** Updates an existing ContactMechType record.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechTypeId | String (id) | Yes | Identifier of the type to update. |
| parentTypeId | String (id) | No | Updated parent type. |
| hasTable | Indicator (Y/N) | No | Updated has-table flag. |
| description | String (description) | No | Updated description. |

**Outputs:** _(Standard success/error response only.)_

**Side Effects:** The specified `ContactMechType` record is updated in place.

---

### 2.17 `deleteContactMechType`

**Description:** Deletes a ContactMechType record.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechTypeId | String (id) | Yes | Identifier of the type to delete. |

**Outputs:** _(Standard success/error response only.)_

**Side Effects:** The specified `ContactMechType` record is removed.

---

### 2.18 `createContactMechPurposeType`

**Description:** Creates a new ContactMechPurposeType record that classifies the purpose of a contact mechanism (e.g. PRIMARY_EMAIL, PHONE_WORK).

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechPurposeTypeId | String (id) | No | Caller-supplied identifier; auto-generated if omitted. |
| parentTypeId | String (id) | No | Parent purpose type. |
| hasTable | Indicator (Y/N) | No | Has-table flag. |
| description | String (description) | No | Human-readable description. |

**Outputs:**

| Field | Type | Description |
|---|---|---|
| contactMechPurposeTypeId | String (id) | Identifier of the newly created purpose type. |

**Side Effects:** One new `ContactMechPurposeType` record.

---

### 2.19 `updateContactMechPurposeType`

**Description:** Updates an existing ContactMechPurposeType record.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechPurposeTypeId | String (id) | Yes | Identifier of the purpose type to update. |
| parentTypeId | String (id) | No | Updated parent type. |
| hasTable | Indicator (Y/N) | No | Updated flag. |
| description | String (description) | No | Updated description. |

**Outputs:** _(Standard success/error response only.)_

**Side Effects:** The specified `ContactMechPurposeType` record is updated in place.

---

### 2.20 `deleteContactMechPurposeType`

**Description:** Deletes a ContactMechPurposeType record.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechPurposeTypeId | String (id) | Yes | Identifier of the purpose type to delete. |

**Outputs:** _(Standard success/error response only.)_

**Side Effects:** The specified `ContactMechPurposeType` record is removed.

---

### 2.21 `createContactMechTypeAttr`

**Description:** Creates a ContactMechTypeAttr record that declares a named attribute applicable to a specific ContactMechType.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechTypeId | String (id) | Yes | The ContactMechType this attribute belongs to. |
| attrName | String (id-long) | Yes | Name of the attribute. |
| description | String (description) | No | Human-readable description. |

**Outputs:** _(Standard success/error response only.)_

**Side Effects:** One new `ContactMechTypeAttr` record.

---

### 2.22 `updateContactMechTypeAttr`

**Description:** Updates an existing ContactMechTypeAttr record.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechTypeId | String (id) | Yes | Type identifier (part of composite primary key). |
| attrName | String (id-long) | Yes | Attribute name (part of composite primary key). |
| description | String (description) | No | Updated description. |

**Outputs:** _(Standard success/error response only.)_

**Side Effects:** The specified `ContactMechTypeAttr` record is updated.

---

### 2.23 `deleteContactMechTypeAttr`

**Description:** Deletes a ContactMechTypeAttr record.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechTypeId | String (id) | Yes | Type identifier. |
| attrName | String (id-long) | Yes | Attribute name. |

**Outputs:** _(Standard success/error response only.)_

**Side Effects:** The specified `ContactMechTypeAttr` record is removed.

---

### 2.24 `createContactMechTypePurpose`

**Description:** Creates a ContactMechTypePurpose record that declares a valid pairing of a ContactMechType and a ContactMechPurposeType.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechTypeId | String (id) | Yes | The ContactMechType. |
| contactMechPurposeTypeId | String (id) | Yes | The ContactMechPurposeType. |

**Outputs:** _(Standard success/error response only.)_

**Side Effects:** One new `ContactMechTypePurpose` record.

---

### 2.25 `updateContactMechTypePurpose`

**Description:** Updates an existing ContactMechTypePurpose record. Because this entity has no non-key fields, the practical effect is to confirm the record exists.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechTypeId | String (id) | Yes | Type identifier. |
| contactMechPurposeTypeId | String (id) | Yes | Purpose type identifier. |

**Outputs:** _(Standard success/error response only.)_

**Side Effects:** The specified `ContactMechTypePurpose` record is updated (no field changes expected given the all-PK structure).

---

### 2.26 `deleteContactMechTypePurpose`

**Description:** Deletes a ContactMechTypePurpose record, removing the declared pairing.

**Inputs:**

| Field | Type | Required | Description |
|---|---|---|---|
| contactMechTypeId | String (id) | Yes | Type identifier. |
| contactMechPurposeTypeId | String (id) | Yes | Purpose type identifier. |

**Outputs:** _(Standard success/error response only.)_

**Side Effects:** The specified `ContactMechTypePurpose` record is removed.

---

## 3. Framework Integration

### 3.1 Infrastructure inputs on every service call

| Framework name | Logical role | Role in services |
|---|---|---|
| `userLogin` | Authenticated user | Required on all authenticated service calls. The framework uses it to establish identity and check permissions. The service logic itself does not read this value directly. |
| `locale` | Locale | Used to look up localised error and success messages from `PartyUiLabels` and `ServiceErrorUiLabels` resource bundles. |
| `delegator` | Storage layer access | The framework entity-access DSL (`from(...)`, `makeValue(...)`, `.create()`, `.store()`) operates through the delegator. Services do not reference it explicitly but it is required by the runtime. |
| `dispatcher` | Service dispatcher | Used in `updateFtpAddressWithHistory` and `updatePartyFtpAddress` to call `makeValidContext` when constructing parameters for delegated service calls. All other inter-service calls use the `run service:` DSL. |

### 3.2 How services are invoked and how results are returned

- Services are declared in `applications/contactmech/servicedef/services.xml`.
- Services implemented in Groovy are declared with `engine="groovy"` and a `location` path. Entity-auto services use `engine="entity-auto"`.
- All authenticated services carry `auth="true"`.
- Service parameters are available inside Groovy scripts as the `parameters` map.
- Results are returned as a Groovy `Map`. Successful results are built with `success(message)` or `success()`. Error results use `error(message)`. Both helpers produce the standard OFBiz response map structure (with `responseMessage` and `errorMessageList` keys).
- INOUT parameters appear in both the input `parameters` map and must be included in the returned map.
- The `run service: 'serviceName', with: inputMap` DSL invokes other services synchronously within the same transaction.

### 3.3 Permission and security model

| Service | Permission check |
|---|---|
| `createFtpAddress` | `partyBasePermissionCheck` with action `CREATE` |
| `updateFtpAddressWithHistory` | `partyBasePermissionCheck` with action `UPDATE` |
| All others | `auth="true"` — caller must be authenticated; no additional permission service. |

`partyBasePermissionCheck` is an external service defined in the Party module.

### 3.4 Entity names and storage layer references

| Entity name | Description |
|---|---|
| `ContactMech` | Root record for any contact mechanism. |
| `PostalAddress` | Postal address details; one-to-one with `ContactMech` (PK = `contactMechId`). |
| `TelecomNumber` | Telecommunications number details; one-to-one with `ContactMech`. |
| `FtpAddress` | FTP server details; one-to-one with `ContactMech`. |
| `ContactMechType` | Lookup table of contact mechanism type codes. |
| `ContactMechPurposeType` | Lookup table of purpose codes for contact mechanisms. |
| `ContactMechTypeAttr` | Declares named attributes for a given ContactMechType. |
| `ContactMechTypePurpose` | Declares valid ContactMechType / ContactMechPurposeType pairings. |
| `ContactMechAttribute` | Instance-level attribute values for a specific ContactMech. |
| `ContactMechLink` | Link between two ContactMech records. |
| `EmailAddressVerification` | Holds verification hashes and expiry dates for email verification flows. |
| `PartyContactMech` | Association between a Party and a ContactMech (external — Party module). |
| `PartyContactMechPurpose` | Declares the purpose of a Party's ContactMech association (external — Party module). |
| `ProductStoreEmailSetting` | E-commerce store email template settings; keyed by `emailType` (external — Product module). |
| `WebSite` | Web site record keyed by `productStoreId` (external — Content/WebApp module). |

### 3.5 External services called by this module

| Service name | Module | Used by |
|---|---|---|
| `createPartyContactMech` | Party | `createPartyFtpAddress` |
| `updatePartyContactMech` | Party | `updatePartyFtpAddress` |
| `sendMailFromScreen` | Common / Email | `sendVerifyEmailAddressNotification` |
| `partyBasePermissionCheck` | Party | `createFtpAddress`, `updateFtpAddressWithHistory` |

---

## 4. Glossary

### ContactMech

**Definition:** A single record representing one way to contact a party. The root entity for all address and number types.

**Relationships:** Every PostalAddress, TelecomNumber, and FtpAddress record is a child of a ContactMech (sharing the same `contactMechId` as its primary key). A ContactMech belongs to exactly one ContactMechType. Zero or more parties may reference a ContactMech through PartyContactMech records.

**Example:** `{ contactMechId: "10000", contactMechTypeId: "EMAIL_ADDRESS", infoString: "alice@example.com" }`

---

### contactMechId

**Definition:** A system-assigned or caller-supplied opaque string identifier that uniquely identifies one ContactMech record. Because updates create new records, the same address can have multiple `contactMechId` values over time.

**Relationships:** Primary key of `ContactMech`; also the primary key of `PostalAddress`, `TelecomNumber`, and `FtpAddress` (each shares the same PK as its parent ContactMech).

**Example:** `"10042"`, `"CM_TEST_EMAIL_1"`

---

### ContactMechType

**Definition:** A lookup record that categorises what kind of contact mechanism a ContactMech record is.

**Relationships:** Referenced by `ContactMech.contactMechTypeId`. May have a parent type (`parentTypeId`) for hierarchical classification. Has zero or more `ContactMechTypeAttr` records.

**Example:** `{ contactMechTypeId: "EMAIL_ADDRESS", description: "Email Address" }`

---

### contactMechTypeId

**Definition:** The code that identifies which ContactMechType a ContactMech belongs to. Determines how the address data is stored and which sub-entity (if any) carries the details.

**Relationships:** Foreign key from `ContactMech` to `ContactMechType`.

**Built-in values used by this module:**

| Code | Meaning |
|---|---|
| `EMAIL_ADDRESS` | Email address; data stored in `ContactMech.infoString`. |
| `POSTAL_ADDRESS` | Postal/mailing address; data stored in `PostalAddress`. |
| `TELECOM_NUMBER` | Telephone number; data stored in `TelecomNumber`. |
| `FTP_ADDRESS` | FTP server endpoint; data stored in `FtpAddress`. |
| `WEB_ADDRESS` | Web URL; data stored in `ContactMech.infoString`. |
| `IP_ADDRESS` | IP address; data stored in `ContactMech.infoString`. |
| `ELECTRONIC_ADDRESS` | Generic electronic address; data stored in `ContactMech.infoString`. |
| `DOMAIN_NAME` | Domain name; data stored in `ContactMech.infoString`. |

---

### infoString

**Definition:** A free-text field on the `ContactMech` record used to store the address value for string-based contact mechanism types (email, URL, IP address, domain name, etc.).

**Relationships:** Field on `ContactMech`. For `EMAIL_ADDRESS` type, this is the verbatim email address. For `POSTAL_ADDRESS` and `TELECOM_NUMBER` types, this field is not used; the address data is in the sub-entity.

**Example:** `"alice@example.com"`, `"https://www.example.com"`, `"192.168.1.1"`

---

### PostalAddress

**Definition:** A sub-entity record holding the fields of a physical mailing address. Shares its primary key (`contactMechId`) with the parent `ContactMech` record.

**Relationships:** One-to-one with `ContactMech` where `contactMechTypeId = POSTAL_ADDRESS`. References geographic identifiers (`countryGeoId`, `stateProvinceGeoId`, etc.) that are resolved in the external `Geo` entity.

**Example:** `{ contactMechId: "10001", address1: "123 Main St", city: "Springfield", stateProvinceGeoId: "IL", countryGeoId: "USA", postalCode: "62701" }`

---

### TelecomNumber

**Definition:** A sub-entity record holding the fields of a telephone number. Shares its primary key with the parent `ContactMech` record.

**Relationships:** One-to-one with `ContactMech` where `contactMechTypeId = TELECOM_NUMBER`.

**Example:** `{ contactMechId: "10002", countryCode: "1", areaCode: "415", contactNumber: "555-2000" }`

---

### FtpAddress

**Definition:** A sub-entity record holding connection details for an FTP server. Shares its primary key with the parent `ContactMech` record.

**Relationships:** One-to-one with `ContactMech` where `contactMechTypeId = FTP_ADDRESS`.

**Example:** `{ contactMechId: "10003", hostname: "ftp://files.example.com", username: "ftpuser", passiveMode: "Y", binaryTransfer: "Y" }`

---

### ContactMechPurposeType

**Definition:** A lookup record that classifies the purpose of a contact mechanism in the context of a specific party (e.g. "primary email", "work phone", "billing address").

**Relationships:** Referenced by `PartyContactMechPurpose.contactMechPurposeTypeId`. May be linked to one or more ContactMechTypes via `ContactMechTypePurpose`.

**Example:** `{ contactMechPurposeTypeId: "PRIMARY_EMAIL", description: "Primary Email Address" }`, `{ contactMechPurposeTypeId: "PHONE_WORK", description: "Work Phone" }`, `{ contactMechPurposeTypeId: "GENERAL_LOCATION", description: "General Location" }`

---

### ContactMechTypePurpose

**Definition:** A join record that declares which ContactMechPurposeType values are valid for a given ContactMechType.

**Relationships:** Composite key of `contactMechTypeId` (→ `ContactMechType`) and `contactMechPurposeTypeId` (→ `ContactMechPurposeType`).

**Example:** A record `{ contactMechTypeId: "EMAIL_ADDRESS", contactMechPurposeTypeId: "PRIMARY_EMAIL" }` declares that "Primary Email" is a valid purpose for email-address contact mechanisms.

---

### ContactMechTypeAttr

**Definition:** Declares a named attribute (a key) that is applicable to a particular ContactMechType. Individual ContactMech records store their attribute values in `ContactMechAttribute`.

**Relationships:** Composite key of `contactMechTypeId` and `attrName`. Related to `ContactMechAttribute` through `attrName`.

**Example:** `{ contactMechTypeId: "TELECOM_NUMBER", attrName: "EXTENSION", description: "Phone extension" }`

---

### ContactMechAttribute

**Definition:** An instance-level name-value pair attached to a specific ContactMech record.

**Relationships:** Composite key of `contactMechId` (→ `ContactMech`) and `attrName`. `attrName` should correspond to a declared `ContactMechTypeAttr` for the type of the parent ContactMech.

**Example:** `{ contactMechId: "10002", attrName: "EXTENSION", attrValue: "204" }`

---

### ContactMechLink

**Definition:** A directed link between two ContactMech records, allowing one contact mechanism to reference another.

**Relationships:** Composite key of `contactMechIdFrom` and `contactMechIdTo`, both foreign keys to `ContactMech`.

**Example:** Linking a primary phone number to an alternate number for the same physical location.

---

### EmailAddressVerification

**Definition:** A transient record that holds a one-time verification hash and its expiry date for verifying that an email address is reachable.

**Relationships:** Keyed by `emailAddress`. The `verifyHash` field has a unique index and is the lookup key used by `verifyEmailAddress`. Referenced by `sendVerifyEmailAddressNotification` to retrieve the hash to include in the verification email.

**Example:** `{ emailAddress: "alice@example.com", verifyHash: "a1b2c3d4e5f6", expireDate: "2026-04-27 00:00:00" }`

---

### verifyHash

**Definition:** A unique, opaque token generated when an `EmailAddressVerification` record is created. The recipient clicks a link containing this token to prove they can receive mail at that address.

**Relationships:** Field on `EmailAddressVerification`, uniquely indexed. Used as the input to `verifyEmailAddress`.

**Example:** `"a1b2c3d4e5f6"`

---

### PartyContactMech

**Definition:** An association record linking a party to a ContactMech with a validity period (`fromDate` / `thruDate`). Represents the fact that a party uses a particular contact mechanism.

**Relationships:** Composite key of `partyId`, `contactMechId`, `fromDate`. Part of the Party module. Created and updated by external services `createPartyContactMech` and `updatePartyContactMech`.

**Example:** `{ partyId: "DemoCustomer", contactMechId: "10000", fromDate: "2001-05-13 00:00:00", allowSolicitation: "Y" }`

---

### PartyContactMechPurpose

**Definition:** Refines a `PartyContactMech` association by assigning a purpose (via `contactMechPurposeTypeId`) and its own validity period.

**Relationships:** Composite key of `partyId`, `contactMechId`, `contactMechPurposeTypeId`, `fromDate`. Part of the Party module.

**Example:** `{ partyId: "DemoCustomer", contactMechId: "10000", contactMechPurposeTypeId: "PRIMARY_EMAIL", fromDate: "2001-05-13 00:00:00" }`

---

### immutability-with-history pattern

**Definition:** The update strategy used by all address-level update services. When the data on a contact mechanism changes, the existing record is never modified; instead a new ContactMech (with a new `contactMechId`) is created carrying the updated data. Both the new identifier and the old identifier (`oldContactMechId`) are returned so that party associations can be re-pointed.

**Relationships:** Applied by `updateContactMech`, `updatePostalAddress`, `updateTelecomNumber`, `updateFtpAddressWithHistory`. The old records remain queryable.

**Example:** Updating a postal address from "100 Old St" to "200 New Ave" creates a new `ContactMech` + `PostalAddress` pair. The caller receives `contactMechId = "10099"` (new) and `oldContactMechId = "10001"` (original, unchanged in storage).

---

### Geo / geoId

**Definition:** An external entity (in the Common module) representing a geographic region such as a country, state, province, county, city, or postal-code area. Used in PostalAddress to reference named geographic regions rather than storing raw text.

**Relationships:** Referenced by `PostalAddress.countryGeoId`, `stateProvinceGeoId`, `countyGeoId`, `municipalityGeoId`, `cityGeoId`, `postalCodeGeoId`.

**Example:** `geoId = "USA"` (United States), `geoId = "CAN"` (Canada), `geoId = "CA"` (California), `geoId = "IL"` (Illinois).

---

### Indicator

**Definition:** A single-character field type used for boolean-like flags. Valid values are `"Y"` (yes/true) and `"N"` (no/false).

**Relationships:** Used by `FtpAddress.binaryTransfer`, `FtpAddress.passiveMode`, `FtpAddress.zipFile`, and others.

**Example:** `binaryTransfer = "Y"`, `passiveMode = "N"`

---

### ProductStoreEmailSetting

**Definition:** An external entity (Product module) that holds per-email-type template configuration for a product store, including subject line, from-address, and body screen URI.

**Relationships:** Looked up by `emailType = "PRDS_EMAIL_VERIFY"` in `sendVerifyEmailAddressNotification`. References `WebSite` through `productStoreId`.

**Example:** `{ productStoreId: "9000", emailType: "PRDS_EMAIL_VERIFY", subject: "Verify your email", fromAddress: "noreply@store.example.com", bodyScreenLocation: "component://shop/widget/EmailVerifyScreen.xml" }`

---

### PRDS_EMAIL_VERIFY

**Definition:** The `emailType` code for the email-address verification email template stored in `ProductStoreEmailSetting`.

**Relationships:** Used as a filter key when querying `ProductStoreEmailSetting` in `sendVerifyEmailAddressNotification`.

**Example:** `emailType = "PRDS_EMAIL_VERIFY"`

---

### GeoPoint

**Definition:** An external entity storing geographic coordinates (latitude and longitude). Referenced by `PostalAddress.geoPointId`.

**Relationships:** One PostalAddress may optionally reference one GeoPoint.

**Example:** `{ geoPointId: "GP001", latitude: "37.7749", longitude: "-122.4194" }`
