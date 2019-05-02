// Copyright 2019 Oath Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.controller.persistence;


import com.yahoo.slime.ArrayTraverser;
import com.yahoo.slime.Cursor;
import com.yahoo.slime.Inspector;
import com.yahoo.slime.Slime;
import com.yahoo.vespa.hosted.controller.api.integration.dns.Record;
import com.yahoo.vespa.hosted.controller.api.integration.dns.RecordData;
import com.yahoo.vespa.hosted.controller.api.integration.dns.RecordName;
import com.yahoo.vespa.hosted.controller.dns.CreateRecord;
import com.yahoo.vespa.hosted.controller.dns.CreateRecords;
import com.yahoo.vespa.hosted.controller.dns.NameServiceQueue;
import com.yahoo.vespa.hosted.controller.dns.NameServiceRequest;
import com.yahoo.vespa.hosted.controller.dns.RemoveRecords;

import java.util.ArrayList;

/**
 * Serializer for {@link com.yahoo.vespa.hosted.controller.dns.NameServiceQueue}.
 *
 * @author mpolden
 */
public class NameServiceQueueSerializer {

    private static final String requestsField = "requests";
    private static final String requestType = "requestType";
    private static final String recordsField = "records";
    private static final String typeField = "type";
    private static final String nameField = "name";
    private static final String dataField = "data";

    public Slime toSlime(NameServiceQueue queue) {
        var slime = new Slime();
        var root = slime.setObject();
        var array = root.setArray(requestsField);

        for (var request : queue.requests()) {
            var object = array.addObject();

            if (request instanceof CreateRecords) toSlime(object, (CreateRecords) request);
            else if (request instanceof CreateRecord) toSlime(object, (CreateRecord) request);
            else if (request instanceof RemoveRecords) toSlime(object, (RemoveRecords) request);
            else throw new IllegalArgumentException("No serialization defined for request of type " +
                                                    request.getClass().getName());
        }

        return slime;
    }

    public NameServiceQueue fromSlime(Slime slime) {
        var items = new ArrayList<NameServiceRequest>();
        var root = slime.get();
        root.field(requestsField).traverse((ArrayTraverser) (i, object) -> {
            var request = Request.valueOf(object.field(requestType).asString());
            switch (request) {
                case createRecords:
                    items.add(createRecordsFromSlime(object));
                    break;
                case createRecord:
                    items.add(createRecordFromSlime(object));
                    break;
                case removeRecords:
                    items.add(removeRecordsFromSlime(object));
                    break;
                default: throw new IllegalArgumentException("No serialization defined for request " + request);
            }
        });
        return new NameServiceQueue(items);
    }

    private void toSlime(Cursor object, CreateRecord createRecord) {
        object.setString(requestType, Request.createRecord.name());
        toSlime(object, createRecord.record());
    }

    private void toSlime(Cursor object, CreateRecords createRecords) {
        object.setString(requestType, Request.createRecords.name());
        var recordArray = object.setArray(recordsField);
        createRecords.records().forEach(record -> toSlime(recordArray.addObject(), record));
    }

    private void toSlime(Cursor object, RemoveRecords removeRecords) {
        object.setString(requestType, Request.removeRecords.name());
        object.setString(typeField, removeRecords.type().name());
        removeRecords.name().ifPresent(name -> object.setString(nameField, name.asString()));
        removeRecords.data().ifPresent(data -> object.setString(dataField, data.asString()));
    }

    private void toSlime(Cursor object, Record record) {
        object.setString(typeField, record.type().name());
        object.setString(nameField, record.name().asString());
        object.setString(dataField, record.data().asString());
    }

    private CreateRecords createRecordsFromSlime(Inspector object) {
        var records = new ArrayList<Record>();
        object.field(recordsField).traverse((ArrayTraverser) (i, recordObject) -> records.add(recordFromSlime(recordObject)));
        return new CreateRecords(records);
    }

    private CreateRecord createRecordFromSlime(Inspector object) {
        return new CreateRecord(recordFromSlime(object));
    }

    private RemoveRecords removeRecordsFromSlime(Inspector object) {
        var type = Record.Type.valueOf(object.field(typeField).asString());
        var name = Serializers.optionalField(object.field(nameField), RecordName::from);
        var data = Serializers.optionalField(object.field(dataField), RecordData::from);
        return new RemoveRecords(type, name, data);
    }

    private Record recordFromSlime(Inspector object) {
        return new Record(Record.Type.valueOf(object.field(typeField).asString()),
                          RecordName.from(object.field(nameField).asString()),
                          RecordData.from(object.field(dataField).asString()));
    }

    private enum Request {
        createRecord,
        createRecords,
        removeRecords,
    }

}