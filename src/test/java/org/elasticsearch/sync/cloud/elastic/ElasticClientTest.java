package org.elasticsearch.sync.cloud.elastic;

import org.elasticsearch.sync.cloud.elastic.ElasticClient;
import org.elasticsearch.test.ESIntegTestCase;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ElasticClientTest extends ESIntegTestCase {

    @Test
    public void testGetIndices() {
        if(!indexExists("logs-2019-01-01"))
            createIndex("logs-2019-01-01");

        if(!indexExists("some-index"))
            createIndex("some-index");

        ElasticClient client = new ElasticClient(client().admin());
        Map<String, Long> indices = client.getIndices("logs");
        assertEquals(1,indices.size());
    }

    @Test
    public void testRepositories() throws IOException {
        ElasticClient client = new ElasticClient(client().admin());
        assertEquals(false,client.hasRepository("test_backup1"));
        client.createRepository("fs","test_backup1","test1");
        assertEquals(true,client.hasRepository("test_backup1"));
        client.deleteRepository("test_backup1");
        assertEquals(false,client.hasRepository("test_backup1"));
    }

    @Test
    public void testTakeSnapshot() throws IOException {
        if(!indexExists("logs-2019-01-01"))
            createIndex("logs-2019-01-01");
        ElasticClient client = new ElasticClient(client().admin());

        client.createRepository("fs","test_backup","test");

        client.takeSnapshot("test_backup","snapshot_1","logs-2019-01-01");
    }

    @Test
    public void testSnapshots() throws Exception {
        if(!indexExists("logs-2019-01-01"))
            createIndex("logs-2019-01-01");

        if(!indexExists("logs-2019-01-02"))
            createIndex("logs-2019-01-02");

        ElasticClient client = new ElasticClient(client().admin());

        client.createRepository("fs","logs_backup","testlogs");
        client.takeSnapshot("logs_backup","snapshot_1","logs-2019-01-01");

        //you need to wait for snapshot to complete before starting next.
        while(true) {
            if(client.snapshotsStatus("logs_backup", "snapshot_1")){
                break;
            }
            Thread.sleep(100);
        }

        client.takeSnapshot("logs_backup","snapshot_2","logs-2019-01-02");
        client.snapshotsStatus("logs_backup","snapshot_2");

        while(true) {
            if(client.snapshotsStatus("logs_backup", "snapshot_2")){
                break;
            }
            Thread.sleep(100);
        }
        List<String> list = client.listSnapshots("logs_backup");
        assertEquals(2,list.size());
        for(String ssName: list) {
            System.out.println("Sanpshot name : "+ssName);
        }

        assertEquals(2,client.snapshotsCount("logs_backup","_all"));
        client.deleteSnapshot("logs_backup","snapshot_1");
        client.deleteSnapshot("logs_backup","snapshot_2");
        assertEquals(0,client.snapshotsCount("logs_backup","_all"));


    }



}
