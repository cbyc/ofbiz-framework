/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ofbiz.contactmech

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil

// ---------------------------------------------------------------------------
// createContactMech
// ---------------------------------------------------------------------------

Map createContactMech() {
    GenericValue contactMech = makeValue('ContactMech')
    contactMech.contactMechId = parameters.contactMechId ?: delegator.getNextSeqId('ContactMech')
    contactMech.contactMechTypeId = parameters.contactMechTypeId
    contactMech.infoString = parameters.infoString
    contactMech.create()
    return success([contactMechId: contactMech.contactMechId])
}

// ---------------------------------------------------------------------------
// updateContactMech
// ---------------------------------------------------------------------------

Map updateContactMech() {
    GenericValue contactMech = from('ContactMech').where('contactMechId', parameters.contactMechId).queryOne()
    if (!contactMech) {
        return error(UtilProperties.getMessage('ServiceErrorUiLabels', 'ServiceValueNotFound', parameters.locale))
    }
    if (contactMech.infoString != parameters.infoString) {
        Map createResult = run service: 'createContactMech', with: [
                contactMechTypeId: parameters.contactMechTypeId ?: contactMech.contactMechTypeId,
                infoString: parameters.infoString
        ]
        if (ServiceUtil.isError(createResult)) {
            return createResult
        }
        return success([contactMechId: createResult.contactMechId])
    }
    return success([contactMechId: parameters.contactMechId])
}

// ---------------------------------------------------------------------------
// createPostalAddress
// ---------------------------------------------------------------------------

Map createPostalAddress() {
    if (parameters.countryGeoId == 'USA' && !parameters.stateProvinceGeoId) {
        return error(UtilProperties.getMessage('PartyUiLabels', 'PartyStateInUsMissing', parameters.locale))
    }
    if (parameters.countryGeoId == 'CAN' && !parameters.stateProvinceGeoId) {
        return error(UtilProperties.getMessage('PartyUiLabels', 'PartyProvinceInCanadaMissing', parameters.locale))
    }
    Map createResult = run service: 'createContactMech', with: [contactMechTypeId: 'POSTAL_ADDRESS']
    if (ServiceUtil.isError(createResult)) {
        return createResult
    }
    String contactMechId = createResult.contactMechId

    GenericValue postalAddress = makeValue('PostalAddress')
    postalAddress.contactMechId = contactMechId
    postalAddress.setNonPKFields(parameters)
    postalAddress.create()

    return success([contactMechId: contactMechId])
}

// ---------------------------------------------------------------------------
// updatePostalAddress
// ---------------------------------------------------------------------------

Map updatePostalAddress() {
    if (parameters.countryGeoId == 'USA' && !parameters.stateProvinceGeoId) {
        return error(UtilProperties.getMessage('PartyUiLabels', 'PartyStateInUsMissing', parameters.locale))
    }
    if (parameters.countryGeoId == 'CAN' && !parameters.stateProvinceGeoId) {
        return error(UtilProperties.getMessage('PartyUiLabels', 'PartyProvinceInCanadaMissing', parameters.locale))
    }

    String oldContactMechId = parameters.contactMechId
    GenericValue existingPostalAddress = from('PostalAddress').where('contactMechId', oldContactMechId).queryOne()
    if (!existingPostalAddress) {
        return error(UtilProperties.getMessage('ServiceErrorUiLabels', 'ServiceValueNotFound', parameters.locale))
    }

    boolean postalChanged = ['toName', 'attnName', 'address1', 'address2', 'houseNumber', 'houseNumberExt',
                             'directions', 'city', 'cityGeoId', 'postalCode', 'postalCodeExt',
                             'postalCodeGeoId', 'countryGeoId', 'stateProvinceGeoId', 'countyGeoId',
                             'municipalityGeoId', 'geoPointId'].any { field ->
        existingPostalAddress[field] != parameters[field]
    }

    if (postalChanged) {
        Map createResult = run service: 'createPostalAddress', with: parameters
        if (ServiceUtil.isError(createResult)) {
            return createResult
        }
        return success([contactMechId: createResult.contactMechId, oldContactMechId: oldContactMechId])
    }

    Map updateCmResult = run service: 'updateContactMech', with: [
            contactMechId: oldContactMechId,
            contactMechTypeId: 'POSTAL_ADDRESS',
            infoString: null
    ]
    if (ServiceUtil.isError(updateCmResult)) {
        return updateCmResult
    }
    String newContactMechId = updateCmResult.contactMechId
    if (newContactMechId != oldContactMechId) {
        GenericValue newPostalAddress = makeValue('PostalAddress')
        newPostalAddress.contactMechId = newContactMechId
        newPostalAddress.setNonPKFields(existingPostalAddress)
        newPostalAddress.create()
    }
    return success([contactMechId: newContactMechId, oldContactMechId: oldContactMechId])
}

// ---------------------------------------------------------------------------
// createTelecomNumber
// ---------------------------------------------------------------------------

Map createTelecomNumber() {
    Map createResult = run service: 'createContactMech', with: [contactMechTypeId: 'TELECOM_NUMBER']
    if (ServiceUtil.isError(createResult)) {
        return createResult
    }
    String contactMechId = createResult.contactMechId

    GenericValue telecomNumber = makeValue('TelecomNumber')
    telecomNumber.contactMechId = contactMechId
    telecomNumber.setNonPKFields(parameters)
    telecomNumber.create()

    return success([contactMechId: contactMechId])
}

// ---------------------------------------------------------------------------
// updateTelecomNumber
// ---------------------------------------------------------------------------

Map updateTelecomNumber() {
    String oldContactMechId = parameters.contactMechId
    GenericValue existingTelecom = from('TelecomNumber').where('contactMechId', oldContactMechId).queryOne()
    if (!existingTelecom) {
        return error(UtilProperties.getMessage('ServiceErrorUiLabels', 'ServiceValueNotFound', parameters.locale))
    }

    boolean telecomChanged = ['countryCode', 'areaCode', 'contactNumber', 'askForName'].any { field ->
        existingTelecom[field] != parameters[field]
    }

    if (telecomChanged) {
        Map createResult = run service: 'createTelecomNumber', with: parameters
        if (ServiceUtil.isError(createResult)) {
            return createResult
        }
        return success([contactMechId: createResult.contactMechId, oldContactMechId: oldContactMechId])
    }

    Map updateCmResult = run service: 'updateContactMech', with: [
            contactMechId: oldContactMechId,
            contactMechTypeId: 'TELECOM_NUMBER',
            infoString: null
    ]
    if (ServiceUtil.isError(updateCmResult)) {
        return updateCmResult
    }
    String newContactMechId = updateCmResult.contactMechId
    if (newContactMechId != oldContactMechId) {
        GenericValue newTelecom = makeValue('TelecomNumber')
        newTelecom.contactMechId = newContactMechId
        newTelecom.setNonPKFields(existingTelecom)
        newTelecom.create()
    }
    return success([contactMechId: newContactMechId, oldContactMechId: oldContactMechId])
}

// ---------------------------------------------------------------------------
// createEmailAddress
// ---------------------------------------------------------------------------

Map createEmailAddress() {
    if (!UtilValidate.isEmail(parameters.emailAddress)) {
        return error(UtilProperties.getMessage('PartyUiLabels', 'PartyEmailAddressNotFormattedCorrectly', parameters.locale))
    }
    Map createResult = run service: 'createContactMech', with: [
            contactMechTypeId: 'EMAIL_ADDRESS',
            infoString: parameters.emailAddress
    ]
    if (ServiceUtil.isError(createResult)) {
        return createResult
    }
    return success([contactMechId: createResult.contactMechId])
}

// ---------------------------------------------------------------------------
// updateEmailAddress
// ---------------------------------------------------------------------------

Map updateEmailAddress() {
    if (!UtilValidate.isEmail(parameters.emailAddress)) {
        return error(UtilProperties.getMessage('PartyUiLabels', 'PartyEmailAddressNotFormattedCorrectly', parameters.locale))
    }
    Map updateResult = run service: 'updateContactMech', with: [
            contactMechId: parameters.contactMechId,
            contactMechTypeId: 'EMAIL_ADDRESS',
            infoString: parameters.emailAddress
    ]
    if (ServiceUtil.isError(updateResult)) {
        return updateResult
    }
    return success([contactMechId: updateResult.contactMechId])
}

// ---------------------------------------------------------------------------
// createFtpAddress
// ---------------------------------------------------------------------------

Map createFtpAddress() {
    Map createCmResult = run service: 'createContactMech', with: [contactMechTypeId: 'FTP_ADDRESS']
    if (ServiceUtil.isError(createCmResult)) {
        return createCmResult
    }
    String contactMechId = createCmResult.contactMechId
    if (!contactMechId) {
        return error('Error creating contactMech')
    }

    GenericValue ftpAddress = makeValue('FtpAddress')
    ftpAddress.contactMechId = contactMechId
    ftpAddress.setNonPKFields(parameters)
    ftpAddress.create()

    return success([contactMechId: contactMechId])
}

// ---------------------------------------------------------------------------
// updateFtpAddressWithHistory
// ---------------------------------------------------------------------------

Map updateFtpAddressWithHistory() {
    if (!parameters.contactMechId) {
        return success()
    }
    String oldContactMechId = parameters.contactMechId
    GenericValue existingFtp = from('FtpAddress').where('contactMechId', oldContactMechId).queryOne()

    boolean ftpChanged = existingFtp && ['hostname', 'port', 'username', 'ftpPassword',
                                         'binaryTransfer', 'filePath', 'zipFile',
                                         'passiveMode', 'defaultTimeout'].any { field ->
        existingFtp[field] != parameters[field]
    }

    if (ftpChanged) {
        Map createCtx = dispatcher.dispatchContext.makeValidContext('createFtpAddress', ModelService.IN_PARAM, parameters)
        Map createResult = run service: 'createFtpAddress', with: createCtx
        if (ServiceUtil.isError(createResult)) {
            return createResult
        }
        return success([contactMechId: createResult.contactMechId, oldContactMechId: oldContactMechId])
    }

    Map updateCmCtx = dispatcher.dispatchContext.makeValidContext('updateContactMech', ModelService.IN_PARAM, parameters)
    updateCmCtx.contactMechTypeId = 'FTP_ADDRESS'
    Map updateCmResult = run service: 'updateContactMech', with: updateCmCtx
    if (ServiceUtil.isError(updateCmResult)) {
        return updateCmResult
    }
    return success([contactMechId: updateCmResult.contactMechId, oldContactMechId: oldContactMechId])
}

// ---------------------------------------------------------------------------
// createPartyFtpAddress
// ---------------------------------------------------------------------------

Map createPartyFtpAddress() {
    Map createCtx = dispatcher.dispatchContext.makeValidContext('createFtpAddress', ModelService.IN_PARAM, parameters)
    Map createResult = run service: 'createFtpAddress', with: createCtx
    if (ServiceUtil.isError(createResult)) {
        return createResult
    }
    String contactMechId = createResult.contactMechId

    Map partyContactMechCtx = [
            partyId: parameters.partyId,
            contactMechId: contactMechId,
            userLogin: parameters.userLogin
    ]
    Map partyResult = run service: 'createPartyContactMech', with: partyContactMechCtx
    if (ServiceUtil.isError(partyResult)) {
        return partyResult
    }
    return success([contactMechId: contactMechId])
}

// ---------------------------------------------------------------------------
// updatePartyFtpAddress
// ---------------------------------------------------------------------------

Map updatePartyFtpAddress() {
    Map updateCtx = dispatcher.dispatchContext.makeValidContext('updateFtpAddressWithHistory', ModelService.IN_PARAM, parameters)
    Map updateResult = run service: 'updateFtpAddressWithHistory', with: updateCtx
    if (ServiceUtil.isError(updateResult)) {
        return updateResult
    }

    String newContactMechId = updateResult.contactMechId
    String oldContactMechId = updateResult.oldContactMechId
    if (newContactMechId && newContactMechId != oldContactMechId) {
        Map partyCtx = [
                partyId: parameters.partyId,
                contactMechId: newContactMechId,
                oldContactMechId: oldContactMechId,
                userLogin: parameters.userLogin
        ]
        Map partyResult = run service: 'updatePartyContactMech', with: partyCtx
        if (ServiceUtil.isError(partyResult)) {
            return partyResult
        }
    }
    return success([contactMechId: newContactMechId ?: parameters.contactMechId])
}

// ---------------------------------------------------------------------------
// sendVerifyEmailAddressNotification
// ---------------------------------------------------------------------------

Map sendVerifyEmailAddressNotification() {
    GenericValue verification = from('EmailAddressVerification')
            .where('emailAddress', parameters.emailAddress)
            .queryOne()
    if (!verification) {
        return success()
    }

    GenericValue emailSetting = from('ProductStoreEmailSetting')
            .where('emailType', 'PRDS_EMAIL_VERIFY')
            .queryFirst()
    if (!emailSetting) {
        return success()
    }

    Map emailParams = [
            emailAddress: parameters.emailAddress,
            verifyHash: verification.verifyHash,
            bodyScreenUri: emailSetting.bodyScreenLocation,
            subject: emailSetting.subject,
            sendFrom: emailSetting.fromAddress,
            sendTo: parameters.emailAddress
    ]

    List<GenericValue> webSites = from('WebSite')
            .where('productStoreId', emailSetting.productStoreId)
            .queryList()
    if (webSites) {
        emailParams.webSiteId = webSites[0].webSiteId
    }

    run service: 'sendMailFromScreen', with: emailParams
    return success()
}

// ---------------------------------------------------------------------------
// verifyEmailAddress
// ---------------------------------------------------------------------------

Map verifyEmailAddress() {
    GenericValue verification = from('EmailAddressVerification')
            .where('verifyHash', parameters.verifyHash)
            .queryOne()
    if (!verification) {
        return error(UtilProperties.getMessage('PartyUiLabels', 'PartyEmailAddressNotExist', parameters.locale))
    }
    if (verification.expireDate && UtilDateTime.nowTimestamp().after(verification.expireDate)) {
        return error(UtilProperties.getMessage('PartyUiLabels', 'PartyEmailAddressVerificationExpired', parameters.locale))
    }
    return success()
}
