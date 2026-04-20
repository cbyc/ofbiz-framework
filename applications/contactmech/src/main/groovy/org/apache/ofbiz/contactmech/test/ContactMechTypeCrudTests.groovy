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

class ContactMechTypeCrudTests extends OFBizTestCase {

    ContactMechTypeCrudTests(String name) {
        super(name)
    }

    // -------------------------------------------------------------------------
    // updateFtpAddressWithHistory tests
    // -------------------------------------------------------------------------

    void testUpdateFtpAddressWithHistoryNoChange() {
        Map serviceCtx = [
                contactMechId: 'CM_TEST_FTP_1',
                hostname: 'ftp://test.example.com',
                username: 'testuser',
                binaryTransfer: 'Y',
                passiveMode: 'Y',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateFtpAddressWithHistory', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.contactMechId == 'CM_TEST_FTP_1'
        assert serviceResult.oldContactMechId == 'CM_TEST_FTP_1'
    }

    void testUpdateFtpAddressWithHistoryWithChange() {
        Map serviceCtx = [
                contactMechId: 'CM_TEST_FTP_1',
                hostname: 'ftp://updated.example.com',
                username: 'testuser',
                binaryTransfer: 'Y',
                passiveMode: 'Y',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateFtpAddressWithHistory', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        String newContactMechId = serviceResult.contactMechId
        assert newContactMechId != 'CM_TEST_FTP_1'
        assert serviceResult.oldContactMechId == 'CM_TEST_FTP_1'

        GenericValue ftpAddress = from('FtpAddress').where('contactMechId', newContactMechId).queryOne()
        assert ftpAddress
        assert ftpAddress.hostname == 'ftp://updated.example.com'
    }

    // -------------------------------------------------------------------------
    // ContactMechType CRUD
    // -------------------------------------------------------------------------

    void testCreateContactMechType() {
        Map serviceCtx = [
                contactMechTypeId: 'TEST_CM_TYPE',
                description: 'Test Contact Mech Type',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createContactMechType', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue contactMechType = from('ContactMechType').where('contactMechTypeId', 'TEST_CM_TYPE').queryOne()
        assert contactMechType
        assert contactMechType.description == 'Test Contact Mech Type'
    }

    void testUpdateContactMechType() {
        dispatcher.runSync('createContactMechType',
                [contactMechTypeId: 'TEST_CM_TYPE_UPD', description: 'Original', userLogin: userLogin])

        Map serviceCtx = [
                contactMechTypeId: 'TEST_CM_TYPE_UPD',
                description: 'Updated Description',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateContactMechType', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue contactMechType = from('ContactMechType').where('contactMechTypeId', 'TEST_CM_TYPE_UPD').queryOne()
        assert contactMechType.description == 'Updated Description'
    }

    void testDeleteContactMechType() {
        dispatcher.runSync('createContactMechType',
                [contactMechTypeId: 'TEST_CM_TYPE_DEL', description: 'To Delete', userLogin: userLogin])

        Map serviceResult = dispatcher.runSync('deleteContactMechType',
                [contactMechTypeId: 'TEST_CM_TYPE_DEL', userLogin: userLogin])
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue contactMechType = from('ContactMechType').where('contactMechTypeId', 'TEST_CM_TYPE_DEL').queryOne()
        assert contactMechType == null
    }

    // -------------------------------------------------------------------------
    // ContactMechPurposeType CRUD
    // -------------------------------------------------------------------------

    void testCreateContactMechPurposeType() {
        Map serviceCtx = [
                contactMechPurposeTypeId: 'TEST_CM_PURP',
                description: 'Test Purpose Type',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createContactMechPurposeType', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue purposeType = from('ContactMechPurposeType')
                .where('contactMechPurposeTypeId', 'TEST_CM_PURP').queryOne()
        assert purposeType
        assert purposeType.description == 'Test Purpose Type'
    }

    void testUpdateContactMechPurposeType() {
        dispatcher.runSync('createContactMechPurposeType',
                [contactMechPurposeTypeId: 'TEST_CM_PURP_UPD', description: 'Original', userLogin: userLogin])

        Map serviceCtx = [
                contactMechPurposeTypeId: 'TEST_CM_PURP_UPD',
                description: 'Updated Purpose',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateContactMechPurposeType', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue purposeType = from('ContactMechPurposeType')
                .where('contactMechPurposeTypeId', 'TEST_CM_PURP_UPD').queryOne()
        assert purposeType.description == 'Updated Purpose'
    }

    void testDeleteContactMechPurposeType() {
        dispatcher.runSync('createContactMechPurposeType',
                [contactMechPurposeTypeId: 'TEST_CM_PURP_DEL', description: 'To Delete', userLogin: userLogin])

        Map serviceResult = dispatcher.runSync('deleteContactMechPurposeType',
                [contactMechPurposeTypeId: 'TEST_CM_PURP_DEL', userLogin: userLogin])
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue purposeType = from('ContactMechPurposeType')
                .where('contactMechPurposeTypeId', 'TEST_CM_PURP_DEL').queryOne()
        assert purposeType == null
    }

    // -------------------------------------------------------------------------
    // ContactMechTypeAttr CRUD
    // -------------------------------------------------------------------------

    void testCreateContactMechTypeAttr() {
        Map serviceCtx = [
                contactMechTypeId: 'TELECOM_NUMBER',
                attrName: 'TEST_ATTR',
                description: 'A test attribute',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createContactMechTypeAttr', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue typeAttr = from('ContactMechTypeAttr')
                .where('contactMechTypeId', 'TELECOM_NUMBER', 'attrName', 'TEST_ATTR')
                .queryOne()
        assert typeAttr
        assert typeAttr.description == 'A test attribute'
    }

    void testUpdateContactMechTypeAttr() {
        dispatcher.runSync('createContactMechTypeAttr',
                [contactMechTypeId: 'TELECOM_NUMBER', attrName: 'TEST_ATTR_UPD', description: 'Original',
                 userLogin: userLogin])

        Map serviceCtx = [
                contactMechTypeId: 'TELECOM_NUMBER',
                attrName: 'TEST_ATTR_UPD',
                description: 'Updated Attr Desc',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateContactMechTypeAttr', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue typeAttr = from('ContactMechTypeAttr')
                .where('contactMechTypeId', 'TELECOM_NUMBER', 'attrName', 'TEST_ATTR_UPD')
                .queryOne()
        assert typeAttr.description == 'Updated Attr Desc'
    }

    void testDeleteContactMechTypeAttr() {
        dispatcher.runSync('createContactMechTypeAttr',
                [contactMechTypeId: 'TELECOM_NUMBER', attrName: 'TEST_ATTR_DEL', userLogin: userLogin])

        Map serviceResult = dispatcher.runSync('deleteContactMechTypeAttr',
                [contactMechTypeId: 'TELECOM_NUMBER', attrName: 'TEST_ATTR_DEL', userLogin: userLogin])
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue typeAttr = from('ContactMechTypeAttr')
                .where('contactMechTypeId', 'TELECOM_NUMBER', 'attrName', 'TEST_ATTR_DEL')
                .queryOne()
        assert typeAttr == null
    }

    // -------------------------------------------------------------------------
    // ContactMechTypePurpose CRUD
    // -------------------------------------------------------------------------

    void testCreateContactMechTypePurpose() {
        // Create a fresh type and purpose so the link is guaranteed not to pre-exist
        dispatcher.runSync('createContactMechType', [contactMechTypeId: 'TEST_CMTP_TYPE', userLogin: userLogin])
        dispatcher.runSync('createContactMechPurposeType',
                [contactMechPurposeTypeId: 'TEST_CMTP_PURP', userLogin: userLogin])

        Map serviceCtx = [
                contactMechTypeId: 'TEST_CMTP_TYPE',
                contactMechPurposeTypeId: 'TEST_CMTP_PURP',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createContactMechTypePurpose', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue typePurpose = from('ContactMechTypePurpose')
                .where('contactMechTypeId', 'TEST_CMTP_TYPE', 'contactMechPurposeTypeId', 'TEST_CMTP_PURP')
                .queryOne()
        assert typePurpose
    }

    void testUpdateContactMechTypePurpose() {
        dispatcher.runSync('createContactMechType', [contactMechTypeId: 'TEST_CMTP_TYPE_U', userLogin: userLogin])
        dispatcher.runSync('createContactMechPurposeType',
                [contactMechPurposeTypeId: 'TEST_CMTP_PURP_U', userLogin: userLogin])
        dispatcher.runSync('createContactMechTypePurpose',
                [contactMechTypeId: 'TEST_CMTP_TYPE_U', contactMechPurposeTypeId: 'TEST_CMTP_PURP_U',
                 userLogin: userLogin])

        Map serviceCtx = [
                contactMechTypeId: 'TEST_CMTP_TYPE_U',
                contactMechPurposeTypeId: 'TEST_CMTP_PURP_U',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateContactMechTypePurpose', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue typePurpose = from('ContactMechTypePurpose')
                .where('contactMechTypeId', 'TEST_CMTP_TYPE_U', 'contactMechPurposeTypeId', 'TEST_CMTP_PURP_U')
                .queryOne()
        assert typePurpose
    }

    void testDeleteContactMechTypePurpose() {
        dispatcher.runSync('createContactMechType', [contactMechTypeId: 'TEST_CMTP_TYPE_D', userLogin: userLogin])
        dispatcher.runSync('createContactMechPurposeType',
                [contactMechPurposeTypeId: 'TEST_CMTP_PURP_D', userLogin: userLogin])
        dispatcher.runSync('createContactMechTypePurpose',
                [contactMechTypeId: 'TEST_CMTP_TYPE_D', contactMechPurposeTypeId: 'TEST_CMTP_PURP_D',
                 userLogin: userLogin])

        Map serviceResult = dispatcher.runSync('deleteContactMechTypePurpose',
                [contactMechTypeId: 'TEST_CMTP_TYPE_D', contactMechPurposeTypeId: 'TEST_CMTP_PURP_D',
                 userLogin: userLogin])
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue typePurpose = from('ContactMechTypePurpose')
                .where('contactMechTypeId', 'TEST_CMTP_TYPE_D', 'contactMechPurposeTypeId', 'TEST_CMTP_PURP_D')
                .queryOne()
        assert typePurpose == null
    }

}
