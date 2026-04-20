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

// ===========================================================================
// Adapter-layer imports — only this layer may reference org.apache.ofbiz.*
// ===========================================================================
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.Delegator
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil

// ===========================================================================
// LAYER 1: Domain Model — zero OFBiz imports
// ===========================================================================

interface LogicResult { }

class ContactMechVO {

    String contactMechId, contactMechTypeId, infoString

}

class PostalAddressVO {

    String contactMechId, toName, attnName, address1, address2, houseNumberExt
    Long   houseNumber
    String directions, city, cityGeoId, postalCode, postalCodeExt, postalCodeGeoId
    String countryGeoId, stateProvinceGeoId, countyGeoId, municipalityGeoId, geoPointId

}

class TelecomNumberVO {

    String contactMechId, countryCode, areaCode, contactNumber, askForName

}

class FtpAddressVO {

    String contactMechId, hostname, username, ftpPassword
    Long   port
    String binaryTransfer, filePath, zipFile, passiveMode
    Long   defaultTimeout

}

class CreateResult implements LogicResult {

    String contactMechId

}

class UpdateResult implements LogicResult {

    String contactMechId, oldContactMechId

}

class DomainError implements LogicResult {

    String  messageKey
    boolean literal

    DomainError(String key, boolean literal = false) {
        this.messageKey = key
        this.literal    = literal
    }

}

// ===========================================================================
// LAYER 2: Core Logic — repository interfaces + business-rule classes
// ===========================================================================

interface ContactMechRepository {

    Optional<ContactMechVO> findById(String id)
    String create(ContactMechVO cm)

}

interface PostalAddressRepository {

    Optional<PostalAddressVO> findById(String id)
    void save(PostalAddressVO pa)

}

interface TelecomNumberRepository {

    Optional<TelecomNumberVO> findById(String id)
    void save(TelecomNumberVO tn)

}

interface FtpAddressRepository {

    Optional<FtpAddressVO> findById(String id)
    void save(FtpAddressVO fa)

}

// ---------------------------------------------------------------------------
// 2.1 / 2.2  Generic ContactMech logic
// ---------------------------------------------------------------------------
class ContactMechLogic {

    private final ContactMechRepository repo

    ContactMechLogic(ContactMechRepository repo) { this.repo = repo }

    LogicResult updateContactMech(String contactMechId, String infoString, String contactMechTypeId) {
        Optional<ContactMechVO> existing = repo.findById(contactMechId)
        if (!existing.isPresent()) { return new DomainError('ServiceValueNotFound') }
        ContactMechVO old = existing.get()
        if (old.infoString == infoString) {
            return new UpdateResult(contactMechId: contactMechId, oldContactMechId: contactMechId)
        }
        String newId = repo.create(new ContactMechVO(
            contactMechTypeId: contactMechTypeId ?: old.contactMechTypeId,
            infoString: infoString
        ))
        return new UpdateResult(contactMechId: newId, oldContactMechId: contactMechId)
    }

}

// ---------------------------------------------------------------------------
// 2.3 / 2.4  Postal address logic
// ---------------------------------------------------------------------------
class PostalAddressLogic {

    private final ContactMechRepository cmRepo
    private final PostalAddressRepository paRepo

    PostalAddressLogic(ContactMechRepository cmRepo, PostalAddressRepository paRepo) {
        this.cmRepo = cmRepo
        this.paRepo = paRepo
    }

    LogicResult createPostalAddress(PostalAddressVO pa) {
        DomainError err = validateState(pa.countryGeoId, pa.stateProvinceGeoId)
        if (err) { return err }
        String id = cmRepo.create(new ContactMechVO(contactMechTypeId: 'POSTAL_ADDRESS'))
        pa.contactMechId = id
        paRepo.save(pa)
        return new CreateResult(contactMechId: id)
    }

    LogicResult updatePostalAddress(String contactMechId, PostalAddressVO newPa) {
        DomainError err = validateState(newPa.countryGeoId, newPa.stateProvinceGeoId)
        if (err) { return err }
        Optional<PostalAddressVO> existingOpt = paRepo.findById(contactMechId)
        if (!existingOpt.isPresent()) { return new DomainError('ServiceValueNotFound') }
        String oldId = contactMechId
        if (postalAddressChanged(existingOpt.get(), newPa)) {
            String newId = cmRepo.create(new ContactMechVO(contactMechTypeId: 'POSTAL_ADDRESS'))
            newPa.contactMechId = newId
            paRepo.save(newPa)
            return new UpdateResult(contactMechId: newId, oldContactMechId: oldId)
        }
        return new UpdateResult(contactMechId: oldId, oldContactMechId: oldId)
    }

    private DomainError validateState(String countryGeoId, String stateProvinceGeoId) {
        if (countryGeoId == 'USA' && !stateProvinceGeoId) { return new DomainError('PartyStateInUsMissing') }
        if (countryGeoId == 'CAN' && !stateProvinceGeoId) { return new DomainError('PartyProvinceInCanadaMissing') }
        return null
    }

    private boolean postalAddressChanged(PostalAddressVO a, PostalAddressVO b) {
        a.address1          != b.address1          || a.address2          != b.address2          ||
        a.city              != b.city              || a.postalCode         != b.postalCode         ||
        a.countryGeoId      != b.countryGeoId      || a.stateProvinceGeoId != b.stateProvinceGeoId ||
        a.toName            != b.toName            || a.attnName           != b.attnName           ||
        a.directions        != b.directions        || a.houseNumber        != b.houseNumber        ||
        a.houseNumberExt    != b.houseNumberExt    || a.cityGeoId          != b.cityGeoId          ||
        a.postalCodeExt     != b.postalCodeExt     || a.postalCodeGeoId    != b.postalCodeGeoId    ||
        a.countyGeoId       != b.countyGeoId       || a.municipalityGeoId  != b.municipalityGeoId  ||
        a.geoPointId        != b.geoPointId
    }

}

// ---------------------------------------------------------------------------
// 2.5 / 2.6  Telecom number logic
// ---------------------------------------------------------------------------
class TelecomNumberLogic {

    private final ContactMechRepository  cmRepo
    private final TelecomNumberRepository tnRepo

    TelecomNumberLogic(ContactMechRepository cmRepo, TelecomNumberRepository tnRepo) {
        this.cmRepo = cmRepo
        this.tnRepo = tnRepo
    }

    LogicResult createTelecomNumber(TelecomNumberVO tn) {
        String id = cmRepo.create(new ContactMechVO(contactMechTypeId: 'TELECOM_NUMBER'))
        tn.contactMechId = id
        tnRepo.save(tn)
        return new CreateResult(contactMechId: id)
    }

    LogicResult updateTelecomNumber(String contactMechId, TelecomNumberVO newTn) {
        Optional<TelecomNumberVO> existingOpt = tnRepo.findById(contactMechId)
        if (!existingOpt.isPresent()) { return new DomainError('ServiceValueNotFound') }
        String oldId = contactMechId
        if (telecomNumberChanged(existingOpt.get(), newTn)) {
            String newId = cmRepo.create(new ContactMechVO(contactMechTypeId: 'TELECOM_NUMBER'))
            newTn.contactMechId = newId
            tnRepo.save(newTn)
            return new UpdateResult(contactMechId: newId, oldContactMechId: oldId)
        }
        return new UpdateResult(contactMechId: oldId, oldContactMechId: oldId)
    }

    private boolean telecomNumberChanged(TelecomNumberVO a, TelecomNumberVO b) {
        a.countryCode   != b.countryCode   || a.areaCode      != b.areaCode ||
        a.contactNumber != b.contactNumber || a.askForName     != b.askForName
    }

}

// ---------------------------------------------------------------------------
// 2.9 / 2.10  FTP address logic
// ---------------------------------------------------------------------------
class FtpAddressLogic {

    private final ContactMechRepository cmRepo
    private final FtpAddressRepository  ftpRepo

    FtpAddressLogic(ContactMechRepository cmRepo, FtpAddressRepository ftpRepo) {
        this.cmRepo  = cmRepo
        this.ftpRepo = ftpRepo
    }

    LogicResult createFtpAddress(FtpAddressVO ftp) {
        String id = cmRepo.create(new ContactMechVO(contactMechTypeId: 'FTP_ADDRESS'))
        if (!id) { return new DomainError('Error creating contactMech', true) }
        ftp.contactMechId = id
        ftpRepo.save(ftp)
        return new CreateResult(contactMechId: id)
    }

    LogicResult updateFtpAddressWithHistory(String contactMechId, FtpAddressVO newFtp) {
        if (!contactMechId) { return new UpdateResult(contactMechId: null, oldContactMechId: null) }
        Optional<FtpAddressVO> existingOpt = ftpRepo.findById(contactMechId)
        String oldId = contactMechId
        if (existingOpt.isPresent() && ftpAddressChanged(existingOpt.get(), newFtp)) {
            String newId = cmRepo.create(new ContactMechVO(contactMechTypeId: 'FTP_ADDRESS'))
            newFtp.contactMechId = newId
            ftpRepo.save(newFtp)
            return new UpdateResult(contactMechId: newId, oldContactMechId: oldId)
        }
        return new UpdateResult(contactMechId: oldId, oldContactMechId: oldId)
    }

    private boolean ftpAddressChanged(FtpAddressVO a, FtpAddressVO b) {
        a.hostname       != b.hostname       || a.port           != b.port           ||
        a.username       != b.username       || a.ftpPassword    != b.ftpPassword    ||
        a.binaryTransfer != b.binaryTransfer || a.filePath       != b.filePath       ||
        a.zipFile        != b.zipFile        || a.passiveMode    != b.passiveMode    ||
        a.defaultTimeout != b.defaultTimeout
    }

}

// ===========================================================================
// LAYER 3: Adapter — OFBiz-backed repository implementations
// ===========================================================================

class OFBizContactMechRepository implements ContactMechRepository {

    Delegator delegator

    OFBizContactMechRepository(Delegator delegator) { this.delegator = delegator }

    Optional<ContactMechVO> findById(String id) {
        GenericValue gv = delegator.findOne('ContactMech', [contactMechId: id], false)
        if (!gv) { return Optional.empty() }
        return Optional.of(new ContactMechVO(
            contactMechId:    gv.contactMechId,
            contactMechTypeId: gv.contactMechTypeId,
            infoString:       gv.infoString
        ))
    }

    String create(ContactMechVO cm) {
        Delegator d = delegator
        GenericValue gv = d.makeValue('ContactMech')
        gv.with {
            contactMechId     = cm.contactMechId ?: d.getNextSeqId('ContactMech')
            contactMechTypeId = cm.contactMechTypeId
            infoString        = cm.infoString
            create()
        }
        return gv.contactMechId
    }

}

class OFBizPostalAddressRepository implements PostalAddressRepository {

    Delegator delegator

    OFBizPostalAddressRepository(Delegator delegator) { this.delegator = delegator }

    Optional<PostalAddressVO> findById(String id) {
        GenericValue gv = delegator.findOne('PostalAddress', [contactMechId: id], false)
        if (!gv) { return Optional.empty() }
        return Optional.of(new PostalAddressVO(
            contactMechId:      gv.contactMechId,
            toName:             gv.toName,
            attnName:           gv.attnName,
            address1:           gv.address1,
            address2:           gv.address2,
            houseNumber:        gv.getLong('houseNumber'),
            houseNumberExt:     gv.houseNumberExt,
            directions:         gv.directions,
            city:               gv.city,
            cityGeoId:          gv.cityGeoId,
            postalCode:         gv.postalCode,
            postalCodeExt:      gv.postalCodeExt,
            postalCodeGeoId:    gv.postalCodeGeoId,
            countryGeoId:       gv.countryGeoId,
            stateProvinceGeoId: gv.stateProvinceGeoId,
            countyGeoId:        gv.countyGeoId,
            municipalityGeoId:  gv.municipalityGeoId,
            geoPointId:         gv.geoPointId
        ))
    }

    void save(PostalAddressVO pa) {
        GenericValue gv = delegator.makeValue('PostalAddress')
        gv.with {
            contactMechId      = pa.contactMechId
            toName             = pa.toName
            attnName           = pa.attnName
            address1           = pa.address1
            address2           = pa.address2
            if (pa.houseNumber != null) { houseNumber = pa.houseNumber }
            houseNumberExt     = pa.houseNumberExt
            directions         = pa.directions
            city               = pa.city
            cityGeoId          = pa.cityGeoId
            postalCode         = pa.postalCode
            postalCodeExt      = pa.postalCodeExt
            postalCodeGeoId    = pa.postalCodeGeoId
            countryGeoId       = pa.countryGeoId
            stateProvinceGeoId = pa.stateProvinceGeoId
            countyGeoId        = pa.countyGeoId
            municipalityGeoId  = pa.municipalityGeoId
            geoPointId         = pa.geoPointId
            create()
        }
    }

}

class OFBizTelecomNumberRepository implements TelecomNumberRepository {

    Delegator delegator

    OFBizTelecomNumberRepository(Delegator delegator) { this.delegator = delegator }

    Optional<TelecomNumberVO> findById(String id) {
        GenericValue gv = delegator.findOne('TelecomNumber', [contactMechId: id], false)
        if (!gv) { return Optional.empty() }
        return Optional.of(new TelecomNumberVO(
            contactMechId: gv.contactMechId,
            countryCode:   gv.countryCode,
            areaCode:      gv.areaCode,
            contactNumber: gv.contactNumber,
            askForName:    gv.askForName
        ))
    }

    void save(TelecomNumberVO tn) {
        GenericValue gv = delegator.makeValue('TelecomNumber')
        gv.with {
            contactMechId = tn.contactMechId
            countryCode   = tn.countryCode
            areaCode      = tn.areaCode
            contactNumber = tn.contactNumber
            askForName    = tn.askForName
            create()
        }
    }

}

class OFBizFtpAddressRepository implements FtpAddressRepository {

    Delegator delegator

    OFBizFtpAddressRepository(Delegator delegator) { this.delegator = delegator }

    Optional<FtpAddressVO> findById(String id) {
        GenericValue gv = delegator.findOne('FtpAddress', [contactMechId: id], false)
        if (!gv) { return Optional.empty() }
        return Optional.of(new FtpAddressVO(
            contactMechId:  gv.contactMechId,
            hostname:       gv.hostname,
            port:           gv.getLong('port'),
            username:       gv.username,
            ftpPassword:    gv.ftpPassword,
            binaryTransfer: gv.binaryTransfer,
            filePath:       gv.filePath,
            zipFile:        gv.zipFile,
            passiveMode:    gv.passiveMode,
            defaultTimeout: gv.getLong('defaultTimeout')
        ))
    }

    void save(FtpAddressVO fa) {
        GenericValue gv = delegator.makeValue('FtpAddress')
        gv.with {
            contactMechId  = fa.contactMechId
            hostname       = fa.hostname
            if (fa.port != null)           { port           = fa.port }
            username       = fa.username
            ftpPassword    = fa.ftpPassword
            binaryTransfer = fa.binaryTransfer
            filePath       = fa.filePath
            zipFile        = fa.zipFile
            passiveMode    = fa.passiveMode
            if (fa.defaultTimeout != null) { defaultTimeout = fa.defaultTimeout }
            create()
        }
    }

}

// ===========================================================================
// Adapter helpers
// ===========================================================================

Map resolveError(DomainError err) {
    if (err.literal) { return ServiceUtil.returnError(err.messageKey) }
    String resource = (err.messageKey == 'ServiceValueNotFound') ? 'ServiceErrorUiLabels' : 'PartyUiLabels'
    return ServiceUtil.returnError(UtilProperties.getMessage(resource, err.messageKey, locale))
}

Long toLong(Object val) { val != null ? (val as Long) : null }

// ===========================================================================
// Service methods — one per Groovy-backed service entry in services.xml
// ===========================================================================

// 2.2 updateContactMech
Map updateContactMech() {
    ContactMechLogic logic = new ContactMechLogic(new OFBizContactMechRepository(delegator))
    LogicResult result = logic.updateContactMech(
        parameters.contactMechId, parameters.infoString, parameters.contactMechTypeId)
    if (result instanceof DomainError) { return resolveError(result as DomainError) }
    UpdateResult r = result as UpdateResult
    Map out = ServiceUtil.returnSuccess()
    out.contactMechId = r.contactMechId
    return out
}

// 2.3 createPostalAddress
Map createPostalAddress() {
    PostalAddressVO pa = new PostalAddressVO(
        toName:             parameters.toName,
        attnName:           parameters.attnName,
        address1:           parameters.address1,
        address2:           parameters.address2,
        houseNumber:        toLong(parameters.houseNumber),
        houseNumberExt:     parameters.houseNumberExt,
        directions:         parameters.directions,
        city:               parameters.city,
        cityGeoId:          parameters.cityGeoId,
        postalCode:         parameters.postalCode,
        postalCodeExt:      parameters.postalCodeExt,
        postalCodeGeoId:    parameters.postalCodeGeoId,
        countryGeoId:       parameters.countryGeoId,
        stateProvinceGeoId: parameters.stateProvinceGeoId,
        countyGeoId:        parameters.countyGeoId,
        municipalityGeoId:  parameters.municipalityGeoId,
        geoPointId:         parameters.geoPointId
    )
    PostalAddressLogic logic = new PostalAddressLogic(
        new OFBizContactMechRepository(delegator), new OFBizPostalAddressRepository(delegator))
    LogicResult result = logic.createPostalAddress(pa)
    if (result instanceof DomainError) { return resolveError(result as DomainError) }
    CreateResult r = result as CreateResult
    Map out = ServiceUtil.returnSuccess()
    out.contactMechId = r.contactMechId
    return out
}

// 2.4 updatePostalAddress
Map updatePostalAddress() {
    for (String field : ['address1', 'city', 'postalCode']) {
        if (!parameters[field]) {
            return ServiceUtil.returnError(
                UtilProperties.getMessage('ServiceErrorUiLabels', 'ServiceMissingParameter', [parameter: field], locale))
        }
    }
    PostalAddressVO newPa = new PostalAddressVO(
        toName:             parameters.toName,
        attnName:           parameters.attnName,
        address1:           parameters.address1,
        address2:           parameters.address2,
        houseNumber:        toLong(parameters.houseNumber),
        houseNumberExt:     parameters.houseNumberExt,
        directions:         parameters.directions,
        city:               parameters.city,
        cityGeoId:          parameters.cityGeoId,
        postalCode:         parameters.postalCode,
        postalCodeExt:      parameters.postalCodeExt,
        postalCodeGeoId:    parameters.postalCodeGeoId,
        countryGeoId:       parameters.countryGeoId,
        stateProvinceGeoId: parameters.stateProvinceGeoId,
        countyGeoId:        parameters.countyGeoId,
        municipalityGeoId:  parameters.municipalityGeoId,
        geoPointId:         parameters.geoPointId
    )
    PostalAddressLogic logic = new PostalAddressLogic(
        new OFBizContactMechRepository(delegator), new OFBizPostalAddressRepository(delegator))
    LogicResult result = logic.updatePostalAddress(parameters.contactMechId, newPa)
    if (result instanceof DomainError) { return resolveError(result as DomainError) }
    UpdateResult r = result as UpdateResult
    Map out = ServiceUtil.returnSuccess()
    out.contactMechId    = r.contactMechId
    out.oldContactMechId = r.oldContactMechId
    return out
}

// 2.5 createTelecomNumber
Map createTelecomNumber() {
    TelecomNumberVO tn = new TelecomNumberVO(
        countryCode:   parameters.countryCode,
        areaCode:      parameters.areaCode,
        contactNumber: parameters.contactNumber,
        askForName:    parameters.askForName
    )
    TelecomNumberLogic logic = new TelecomNumberLogic(
        new OFBizContactMechRepository(delegator), new OFBizTelecomNumberRepository(delegator))
    LogicResult result = logic.createTelecomNumber(tn)
    if (result instanceof DomainError) { return resolveError(result as DomainError) }
    CreateResult r = result as CreateResult
    Map out = ServiceUtil.returnSuccess()
    out.contactMechId = r.contactMechId
    return out
}

// 2.6 updateTelecomNumber
Map updateTelecomNumber() {
    TelecomNumberVO newTn = new TelecomNumberVO(
        countryCode:   parameters.countryCode,
        areaCode:      parameters.areaCode,
        contactNumber: parameters.contactNumber,
        askForName:    parameters.askForName
    )
    TelecomNumberLogic logic = new TelecomNumberLogic(
        new OFBizContactMechRepository(delegator), new OFBizTelecomNumberRepository(delegator))
    LogicResult result = logic.updateTelecomNumber(parameters.contactMechId, newTn)
    if (result instanceof DomainError) { return resolveError(result as DomainError) }
    UpdateResult r = result as UpdateResult
    Map out = ServiceUtil.returnSuccess()
    out.contactMechId    = r.contactMechId
    out.oldContactMechId = r.oldContactMechId
    return out
}

// 2.7 createEmailAddress
Map createEmailAddress() {
    if (!UtilValidate.isEmail(parameters.emailAddress)) {
        return ServiceUtil.returnError(
            UtilProperties.getMessage('PartyUiLabels', 'PartyEmailAddressNotFormattedCorrectly', locale))
    }
    OFBizContactMechRepository cmRepo = new OFBizContactMechRepository(delegator)
    String id = cmRepo.create(new ContactMechVO(
        contactMechTypeId: 'EMAIL_ADDRESS',
        infoString:        parameters.emailAddress
    ))
    Map out = ServiceUtil.returnSuccess()
    out.contactMechId = id
    return out
}

// 2.8 updateEmailAddress
Map updateEmailAddress() {
    if (!UtilValidate.isEmail(parameters.emailAddress)) {
        return ServiceUtil.returnError(
            UtilProperties.getMessage('PartyUiLabels', 'PartyEmailAddressNotFormattedCorrectly', locale))
    }
    ContactMechLogic logic = new ContactMechLogic(new OFBizContactMechRepository(delegator))
    LogicResult result = logic.updateContactMech(
        parameters.contactMechId, parameters.emailAddress, 'EMAIL_ADDRESS')
    if (result instanceof DomainError) { return resolveError(result as DomainError) }
    UpdateResult r = result as UpdateResult
    Map out = ServiceUtil.returnSuccess()
    out.contactMechId = r.contactMechId
    return out
}

// 2.9 createFtpAddress
Map createFtpAddress() {
    FtpAddressVO ftp = new FtpAddressVO(
        hostname:       parameters.hostname,
        port:           toLong(parameters.port),
        username:       parameters.username,
        ftpPassword:    parameters.ftpPassword,
        binaryTransfer: parameters.binaryTransfer,
        filePath:       parameters.filePath,
        zipFile:        parameters.zipFile,
        passiveMode:    parameters.passiveMode,
        defaultTimeout: toLong(parameters.defaultTimeout)
    )
    FtpAddressLogic logic = new FtpAddressLogic(
        new OFBizContactMechRepository(delegator), new OFBizFtpAddressRepository(delegator))
    LogicResult result = logic.createFtpAddress(ftp)
    if (result instanceof DomainError) { return resolveError(result as DomainError) }
    CreateResult r = result as CreateResult
    Map out = ServiceUtil.returnSuccess()
    out.contactMechId = r.contactMechId
    return out
}

// 2.10 updateFtpAddressWithHistory
Map updateFtpAddressWithHistory() {
    FtpAddressVO newFtp = new FtpAddressVO(
        hostname:       parameters.hostname,
        port:           toLong(parameters.port),
        username:       parameters.username,
        ftpPassword:    parameters.ftpPassword,
        binaryTransfer: parameters.binaryTransfer,
        filePath:       parameters.filePath,
        zipFile:        parameters.zipFile,
        passiveMode:    parameters.passiveMode,
        defaultTimeout: toLong(parameters.defaultTimeout)
    )
    FtpAddressLogic logic = new FtpAddressLogic(
        new OFBizContactMechRepository(delegator), new OFBizFtpAddressRepository(delegator))
    LogicResult result = logic.updateFtpAddressWithHistory(parameters.contactMechId, newFtp)
    if (result instanceof DomainError) { return resolveError(result as DomainError) }
    UpdateResult r = result as UpdateResult
    Map out = ServiceUtil.returnSuccess()
    out.contactMechId    = r.contactMechId
    out.oldContactMechId = r.oldContactMechId
    return out
}

// 2.11 createPartyFtpAddress
Map createPartyFtpAddress() {
    Map ftpCtx    = dispatcher.getDispatchContext().makeValidContext('createFtpAddress', 'IN', parameters)
    Map ftpResult = dispatcher.runSync('createFtpAddress', ftpCtx)
    if (ServiceUtil.isError(ftpResult)) { return ftpResult }

    String contactMechId = ftpResult.contactMechId
    Map partyCtx         = dispatcher.getDispatchContext().makeValidContext('createPartyContactMech', 'IN', parameters)
    partyCtx.contactMechId = contactMechId
    Map partyResult      = dispatcher.runSync('createPartyContactMech', partyCtx)
    if (ServiceUtil.isError(partyResult)) { return partyResult }

    Map out = ServiceUtil.returnSuccess()
    out.contactMechId = contactMechId
    return out
}

// 2.12 updatePartyFtpAddress
Map updatePartyFtpAddress() {
    Map ftpCtx    = dispatcher.getDispatchContext().makeValidContext('updateFtpAddressWithHistory', 'IN', parameters)
    Map ftpResult = dispatcher.runSync('updateFtpAddressWithHistory', ftpCtx)
    if (ServiceUtil.isError(ftpResult)) { return ftpResult }

    String newContactMechId = ftpResult.contactMechId
    String oldContactMechId = ftpResult.oldContactMechId

    if (newContactMechId && newContactMechId != oldContactMechId) {
        Map updateCtx = dispatcher.getDispatchContext().makeValidContext('updatePartyContactMech', 'IN', parameters)
        updateCtx.contactMechId    = newContactMechId
        updateCtx.oldContactMechId = oldContactMechId
        Map updateResult = dispatcher.runSync('updatePartyContactMech', updateCtx)
        if (ServiceUtil.isError(updateResult)) { return updateResult }
    }

    Map out = ServiceUtil.returnSuccess()
    out.contactMechId = newContactMechId
    return out
}

// 2.13 sendVerifyEmailAddressNotification
Map sendVerifyEmailAddressNotification() {
    String emailAddress = parameters.emailAddress

    GenericValue verification = delegator.findOne('EmailAddressVerification', [emailAddress: emailAddress], false)
    if (!verification) { return ServiceUtil.returnSuccess() }

    List<GenericValue> emailSettings = delegator.findByAnd(
        'ProductStoreEmailSetting', [emailType: 'PRDS_EMAIL_VERIFY'], null, false)
    if (!emailSettings) { return ServiceUtil.returnSuccess() }

    GenericValue emailSetting = emailSettings[0]
    String productStoreId = emailSetting.productStoreId

    List<GenericValue> webSites = delegator.findByAnd('WebSite', [productStoreId: productStoreId], null, false)
    String webSiteId = webSites ? webSites[0].webSiteId : null

    Map emailCtx = [
        sendTo:             emailAddress,
        sendFrom:           emailSetting.fromAddress,
        subject:            emailSetting.subject,
        bodyScreenLocation: emailSetting.bodyScreenLocation,
        productStoreId:     productStoreId,
        emailAddress:       emailAddress,
        verifyHash:         verification.verifyHash
    ]
    if (webSiteId) { emailCtx.webSiteId = webSiteId }

    dispatcher.runSync('sendMailFromScreen', emailCtx)
    return ServiceUtil.returnSuccess()
}

// 2.14 verifyEmailAddress
Map verifyEmailAddress() {
    List<GenericValue> results = delegator.findByAnd(
        'EmailAddressVerification', [verifyHash: parameters.verifyHash], null, false)
    if (!results) {
        return ServiceUtil.returnError(
            UtilProperties.getMessage('PartyUiLabels', 'PartyEmailAddressNotExist', locale))
    }
    GenericValue verification = results[0]
    if (verification.expireDate && verification.expireDate.before(new Date())) {
        return ServiceUtil.returnError(
            UtilProperties.getMessage('PartyUiLabels', 'PartyEmailAddressVerificationExpired', locale))
    }
    return ServiceUtil.returnSuccess()
}
