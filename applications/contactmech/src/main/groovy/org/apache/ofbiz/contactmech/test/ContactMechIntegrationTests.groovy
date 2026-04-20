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
package org.apache.ofbiz.contactmech.test

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class ContactMechIntegrationTests extends OFBizTestCase {

    ContactMechIntegrationTests(String name) {
        super(name)
    }

    // -------------------------------------------------------------------------
    // createTelecomNumber tests
    // -------------------------------------------------------------------------

    void testCreateTelecomNumber() {
        Map serviceCtx = [
                areaCode: '415',
                contactNumber: '555-2000',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createTelecomNumber', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String contactMechId = serviceResult.contactMechId
        assert contactMechId

        GenericValue contactMech = from('ContactMech').where('contactMechId', contactMechId).queryOne()
        assert contactMech
        assert contactMech.contactMechTypeId == 'TELECOM_NUMBER'

        GenericValue telecomNumber = from('TelecomNumber').where('contactMechId', contactMechId).queryOne()
        assert telecomNumber
        assert telecomNumber.areaCode == '415'
        assert telecomNumber.contactNumber == '555-2000'
    }

    void testCreateTelecomNumberWithCountryCode() {
        Map serviceCtx = [
                countryCode: '44',
                areaCode: '20',
                contactNumber: '7000-0001',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createTelecomNumber', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String contactMechId = serviceResult.contactMechId
        assert contactMechId

        GenericValue telecomNumber = from('TelecomNumber').where('contactMechId', contactMechId).queryOne()
        assert telecomNumber
        assert telecomNumber.countryCode == '44'
        assert telecomNumber.areaCode == '20'
        assert telecomNumber.contactNumber == '7000-0001'
    }

    void testCreateTelecomNumberWithAskForName() {
        Map serviceCtx = [
                areaCode: '650',
                contactNumber: '555-3000',
                askForName: 'Jane Doe',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createTelecomNumber', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String contactMechId = serviceResult.contactMechId
        assert contactMechId

        GenericValue telecomNumber = from('TelecomNumber').where('contactMechId', contactMechId).queryOne()
        assert telecomNumber
        assert telecomNumber.askForName == 'Jane Doe'
    }

    void testCreateTelecomNumberMinimal() {
        // contactNumber only — all other fields are optional per the service definition
        Map serviceCtx = [
                contactNumber: '555-9999',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createTelecomNumber', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        String contactMechId = serviceResult.contactMechId
        assert contactMechId

        GenericValue telecomNumber = from('TelecomNumber').where('contactMechId', contactMechId).queryOne()
        assert telecomNumber
        assert telecomNumber.contactNumber == '555-9999'
    }

    // -------------------------------------------------------------------------
    // createEmailAddress tests
    // -------------------------------------------------------------------------

    void testCreateEmailAddress() {
        Map serviceCtx = [
                emailAddress: 'integration.test@example.com',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createEmailAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String contactMechId = serviceResult.contactMechId
        assert contactMechId

        GenericValue contactMech = from('ContactMech').where('contactMechId', contactMechId).queryOne()
        assert contactMech
        assert contactMech.contactMechTypeId == 'EMAIL_ADDRESS'
        assert contactMech.infoString == 'integration.test@example.com'
    }

    void testCreateEmailAddressSubdomain() {
        Map serviceCtx = [
                emailAddress: 'user@mail.subdomain.example.org',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createEmailAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        String contactMechId = serviceResult.contactMechId
        assert contactMechId

        GenericValue contactMech = from('ContactMech').where('contactMechId', contactMechId).queryOne()
        assert contactMech
        assert contactMech.infoString == 'user@mail.subdomain.example.org'
    }

    void testCreateEmailAddressInvalidFormat() {
        Map serviceCtx = [
                emailAddress: 'not-a-valid-email',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createEmailAddress', serviceCtx)
        assert ServiceUtil.isError(serviceResult)
    }

    void testCreateEmailAddressMissingAtSign() {
        Map serviceCtx = [
                emailAddress: 'invalidemail.example.com',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createEmailAddress', serviceCtx)
        assert ServiceUtil.isError(serviceResult)
    }

    void testCreateEmailAddressStoredAsInfoString() {
        // Verify the email is stored in infoString on ContactMech, not a separate entity
        String email = 'stored.infostring@example.com'
        Map serviceCtx = [emailAddress: email, userLogin: userLogin]
        Map serviceResult = dispatcher.runSync('createEmailAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue contactMech = from('ContactMech')
                .where('contactMechId', serviceResult.contactMechId)
                .queryOne()
        assert contactMech.infoString == email
        // EMAIL_ADDRESS contacts have no separate sub-entity record
        assert contactMech.contactMechTypeId == 'EMAIL_ADDRESS'
    }

    // -------------------------------------------------------------------------
    // updatePostalAddress tests  (migrated from MiniLang testCreatePostalAddress
    // and expanded to cover updatePostalAddress)
    // -------------------------------------------------------------------------

    void testCreatePostalAddress() {
        Map serviceCtx = [
                toName: 'Test Address',
                address1: '2004 Factory Blvd',
                city: 'City of Industry',
                postalCode: '90000',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPostalAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String contactMechId = serviceResult.contactMechId
        assert contactMechId

        GenericValue postalAddress = from('PostalAddress').where('contactMechId', contactMechId).queryOne()
        assert postalAddress
        assert postalAddress.address1 == '2004 Factory Blvd'
        assert postalAddress.city == 'City of Industry'
        assert postalAddress.postalCode == '90000'
    }

    void testUpdatePostalAddressNoChange() {
        // Create a postal address, then update it with the identical data.
        // The service must succeed and return the same contactMechId (no new record needed).
        Map createCtx = [
                address1: '100 Test Ave',
                city: 'Test City',
                stateProvinceGeoId: 'CA',
                countryGeoId: 'USA',
                postalCode: '90210',
                userLogin: userLogin
        ]
        Map createResult = dispatcher.runSync('createPostalAddress', createCtx)
        assert ServiceUtil.isSuccess(createResult)
        String originalContactMechId = createResult.contactMechId
        assert originalContactMechId

        Map updateCtx = [
                contactMechId: originalContactMechId,
                address1: '100 Test Ave',
                city: 'Test City',
                stateProvinceGeoId: 'CA',
                countryGeoId: 'USA',
                postalCode: '90210',
                userLogin: userLogin
        ]
        Map updateResult = dispatcher.runSync('updatePostalAddress', updateCtx)
        assert ServiceUtil.isSuccess(updateResult)

        String returnedId = updateResult.contactMechId
        assert returnedId
        assert returnedId == originalContactMechId

        GenericValue postalAddress = from('PostalAddress').where('contactMechId', returnedId).queryOne()
        assert postalAddress
        assert postalAddress.address1 == '100 Test Ave'
        assert postalAddress.stateProvinceGeoId == 'CA'
        assert postalAddress.postalCode == '90210'
    }

    void testUpdatePostalAddressWithChange() {
        // Create a postal address, then update it with changed fields.
        // The service must create a new ContactMech/PostalAddress record and
        // return a different contactMechId while oldContactMechId points to the original.
        Map createCtx = [
                address1: '200 Original Rd',
                city: 'Original City',
                stateProvinceGeoId: 'CA',
                countryGeoId: 'USA',
                postalCode: '94000',
                userLogin: userLogin
        ]
        Map createResult = dispatcher.runSync('createPostalAddress', createCtx)
        assert ServiceUtil.isSuccess(createResult)
        String originalContactMechId = createResult.contactMechId
        assert originalContactMechId

        Map updateCtx = [
                contactMechId: originalContactMechId,
                address1: '200 Original Rd',
                city: 'New City',
                stateProvinceGeoId: 'TX',
                countryGeoId: 'USA',
                postalCode: '75001',
                userLogin: userLogin
        ]
        Map updateResult = dispatcher.runSync('updatePostalAddress', updateCtx)
        assert ServiceUtil.isSuccess(updateResult)

        String newContactMechId = updateResult.contactMechId
        assert newContactMechId
        assert newContactMechId != originalContactMechId
        assert updateResult.oldContactMechId == originalContactMechId

        GenericValue newPostalAddress = from('PostalAddress').where('contactMechId', newContactMechId).queryOne()
        assert newPostalAddress
        assert newPostalAddress.city == 'New City'
        assert newPostalAddress.stateProvinceGeoId == 'TX'
        assert newPostalAddress.postalCode == '75001'
    }

    void testUpdatePostalAddressStateChange() {
        // Mirrors the state-change scenario from PartyContactMechTests
        Map createCtx = [
                address1: '300 State Change Ln',
                city: 'Springfield',
                stateProvinceGeoId: 'IL',
                countryGeoId: 'USA',
                postalCode: '62701',
                userLogin: userLogin
        ]
        Map createResult = dispatcher.runSync('createPostalAddress', createCtx)
        assert ServiceUtil.isSuccess(createResult)
        String originalContactMechId = createResult.contactMechId

        Map updateCtx = [
                contactMechId: originalContactMechId,
                address1: '300 State Change Ln',
                city: 'Springfield',
                stateProvinceGeoId: 'VA',
                countryGeoId: 'USA',
                postalCode: '20147',
                userLogin: userLogin
        ]
        Map updateResult = dispatcher.runSync('updatePostalAddress', updateCtx)
        assert ServiceUtil.isSuccess(updateResult)

        String newContactMechId = updateResult.contactMechId
        assert newContactMechId != originalContactMechId
        assert updateResult.oldContactMechId == originalContactMechId

        GenericValue updated = from('PostalAddress').where('contactMechId', newContactMechId).queryOne()
        assert updated.stateProvinceGeoId == 'VA'
        assert updated.postalCode == '20147'
    }

    void testUpdatePostalAddressMissingAddress1() {
        // address1 is required — omitting it should yield an error
        Map serviceCtx = [
                contactMechId: 'CM_TEST_ADDR_1',
                city: 'Somewhere',
                postalCode: '00000',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePostalAddress', serviceCtx)
        assert ServiceUtil.isError(serviceResult)
    }

    // -------------------------------------------------------------------------
    // updateTelecomNumber tests (migrated from MiniLang)
    // -------------------------------------------------------------------------

    void testUpdateTelecomNumberNoChange() {
        Map serviceCtx = [
                contactMechId: 'CM_TEST_TEL_1',
                countryCode: '1',
                areaCode: '801',
                contactNumber: '555-1111',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateTelecomNumber', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        String contactMechId = serviceResult.contactMechId
        assert contactMechId == 'CM_TEST_TEL_1'

        GenericValue telecomNumber = from('TelecomNumber').where('contactMechId', contactMechId).queryOne()
        assert telecomNumber
        assert telecomNumber.areaCode == '801'
        assert telecomNumber.contactNumber == '555-1111'
    }

    void testUpdateTelecomNumberWithChange() {
        Map serviceCtx = [
                contactMechId: 'CM_TEST_TEL_1',
                countryCode: '1',
                areaCode: '801',
                contactNumber: '555-2222',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateTelecomNumber', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        String newContactMechId = serviceResult.contactMechId
        assert newContactMechId != 'CM_TEST_TEL_1'
        assert serviceResult.oldContactMechId == 'CM_TEST_TEL_1'

        GenericValue telecomNumber = from('TelecomNumber').where('contactMechId', newContactMechId).queryOne()
        assert telecomNumber
        assert telecomNumber.contactNumber == '555-2222'
    }

    // -------------------------------------------------------------------------
    // updateEmailAddress / updateContactMech tests (migrated from MiniLang)
    // -------------------------------------------------------------------------

    void testUpdateEmailAddress() {
        Map serviceCtx = [
                contactMechId: 'CM_TEST_EMAIL_1',
                emailAddress: 'updated.contact@example.com',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateEmailAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        String contactMechId = serviceResult.contactMechId
        assert contactMechId

        GenericValue contactMech = from('ContactMech').where('contactMechId', contactMechId).queryOne()
        assert contactMech
        assert contactMech.infoString == 'updated.contact@example.com'
    }

    void testUpdateEmailAddressInvalidFormat() {
        Map serviceCtx = [
                contactMechId: 'CM_TEST_EMAIL_1',
                emailAddress: 'not-an-email',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateEmailAddress', serviceCtx)
        assert ServiceUtil.isError(serviceResult)
    }

    void testUpdateContactMech() {
        Map serviceCtx = [
                contactMechId: 'CM_TEST_GEN_1',
                contactMechTypeId: 'EMAIL_ADDRESS',
                infoString: 'generic.updated@example.com',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateContactMech', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        String contactMechId = serviceResult.contactMechId
        assert contactMechId

        GenericValue contactMech = from('ContactMech').where('contactMechId', contactMechId).queryOne()
        assert contactMech
        assert contactMech.infoString == 'generic.updated@example.com'
    }

    // -------------------------------------------------------------------------
    // createContactMech tests (migrated from MiniLang testCreateContactMech)
    // -------------------------------------------------------------------------

    void testCreateContactMech() {
        Map serviceCtx = [
                contactMechTypeId: 'EMAIL_ADDRESS',
                infoString: 'createcontactmech.test@example.com',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createContactMech', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        String contactMechId = serviceResult.contactMechId
        assert contactMechId

        GenericValue contactMech = from('ContactMech').where('contactMechId', contactMechId).queryOne()
        assert contactMech
        assert contactMech.contactMechTypeId == 'EMAIL_ADDRESS'
        assert contactMech.infoString == 'createcontactmech.test@example.com'
    }

}
