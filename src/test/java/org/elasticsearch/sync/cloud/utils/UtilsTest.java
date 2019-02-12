package org.elasticsearch.sync.cloud.utils;

import org.elasticsearch.test.ESIntegTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UtilsTest extends ESIntegTestCase {

    public void testSnapshots1() throws IOException {
        Map<String,Long> indices = new HashMap<>();

        indices.put("logs-2019-01-01",1234L);
        indices.put("logs-2019-01-02",234L);
        indices.put("logs-2019-01-03",34L);

        String json = Utils.toJson(indices);

        assertNotEquals(null,json);
        List<IndexInfo> list = Utils.toSnapshots(json);
        assertEquals(3,list.size());
    }

    public void testSnapshots2() throws IOException {
        List<IndexInfo> ss = new ArrayList<>();

        ss.add( new IndexInfo("logs-2019-01-01",1234L, IndexInfo.State.READY));
        ss.add( new IndexInfo("logs-2019-01-02",234L, IndexInfo.State.READY));
        ss.add( new IndexInfo("logs-2019-01-03",34L, IndexInfo.State.READY));

        String json = Utils.toJson(ss);

        assertNotEquals(null,json);
        List<IndexInfo> list = Utils.toSnapshots(json);
        assertEquals(3,list.size());
    }


    public void testSnapshotsSortFilter() throws IOException {
        Map<String,Long> indices = new HashMap<>();
        indices.put("logs-2019-01-01",1234L);
        indices.put("logs-2019-01-02",234L);
        indices.put("logs-2019-01-03",34L);

        String json = Utils.toJson(indices);
        List<IndexInfo> ss = Utils.toSnapshots(json);
        List<IndexInfo> filtered = Utils.sortAndFilter(ss, IndexInfo.State.READY);

        assertEquals(3,filtered.size());

        assertEquals("logs-2019-01-03",filtered.get(0).getName());
        assertEquals("logs-2019-01-01",filtered.get(2).getName());


    }

    public void testRemove() throws IOException {
        List<IndexInfo> ss = new ArrayList<>();

        ss.add( new IndexInfo("logs-2019-01-01",1234L, IndexInfo.State.READY));
        ss.add( new IndexInfo("logs-2019-01-02",234L, IndexInfo.State.READY));
        ss.add( new IndexInfo("logs-2019-01-03",34L, IndexInfo.State.READY));

        assertEquals(IndexInfo.State.READY,ss.get(0).getState());
        ss = Utils.replace(new IndexInfo("logs-2019-01-03",34L, IndexInfo.State.SNAPSHOTED),ss);


        assertEquals(3,ss.size());
        assertEquals(IndexInfo.State.SNAPSHOTED,ss.get(0).getState());

    }
}
