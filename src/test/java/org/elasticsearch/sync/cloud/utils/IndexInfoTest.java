package org.elasticsearch.sync.cloud.utils;



import org.elasticsearch.test.ESIntegTestCase;
import org.junit.Test;

import java.io.IOException;



public class IndexInfoTest extends ESIntegTestCase {

    @Test
    public void testSnapshotCreate() throws IOException  {
        IndexInfo indexInfo = new IndexInfo("logs-2019-01-01",Long.valueOf(21345667), IndexInfo.State.SNAPSHOTED);
        assertEquals("logs-2019-01-01", indexInfo.getName());
        assertEquals(21345667L, indexInfo.getSizeInBytes().longValue());
        assertEquals("SNAPSHOTED", indexInfo.getState().toString());

        System.out.println("JSON packet: "+ indexInfo.toJson());

        IndexInfo ss = new IndexInfo(indexInfo.toJson());
        assertEquals("logs-2019-01-01",ss.getName());
        assertEquals(21345667L,ss.getSizeInBytes().longValue());
        assertEquals("SNAPSHOTED",ss.getState().toString());
    }

}
