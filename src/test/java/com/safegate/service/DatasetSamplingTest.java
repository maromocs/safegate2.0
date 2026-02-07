package com.safegate.service;

import com.SafeGate.SafeGateApplication;
import com.SafeGate.model.HttpRequestData;
import com.SafeGate.service.DatasetTestRunnerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SafeGateApplication.class)
@ActiveProfiles("test")
public class DatasetSamplingTest {

    @Autowired
    private DatasetTestRunnerService datasetTestRunnerService;

    @Test
    @SuppressWarnings("unchecked")
    public void testDeterministicSampling() throws Exception {
        // Prepare data
        List<HttpRequestData> requests = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            HttpRequestData req = new HttpRequestData();
            req.setPayload("payload-" + i);
            requests.add(req);
        }

        // Get private method
        Method method = DatasetTestRunnerService.class.getDeclaredMethod("sampleRequests", List.class, String.class, Long.class);
        method.setAccessible(true);

        // Run with seed 42
        List<HttpRequestData> sample1 = (List<HttpRequestData>) method.invoke(datasetTestRunnerService, requests, "Random 100", 42L);
        List<HttpRequestData> sample2 = (List<HttpRequestData>) method.invoke(datasetTestRunnerService, requests, "Random 100", 42L);

        // Verify determinism
        assertEquals(100, sample1.size());
        assertEquals(100, sample2.size());
        for (int i = 0; i < 100; i++) {
            assertEquals(sample1.get(i).getPayload(), sample2.get(i).getPayload(), "Samples should be identical for the same seed");
        }

        // Run with different seed
        List<HttpRequestData> sample3 = (List<HttpRequestData>) method.invoke(datasetTestRunnerService, requests, "Random 100", 43L);
        
        // It's technically possible but highly unlikely that they are identical
        boolean identical = true;
        for (int i = 0; i < 100; i++) {
            if (!sample1.get(i).getPayload().equals(sample3.get(i).getPayload())) {
                identical = false;
                break;
            }
        }
        assertFalse(identical, "Samples should be different for different seeds");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRandom100Option() throws Exception {
        List<HttpRequestData> requests = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            HttpRequestData req = new HttpRequestData();
            req.setPayload("payload-" + i);
            requests.add(req);
        }

        Method method = DatasetTestRunnerService.class.getDeclaredMethod("sampleRequests", List.class, String.class, Long.class);
        method.setAccessible(true);

        List<HttpRequestData> result = (List<HttpRequestData>) method.invoke(datasetTestRunnerService, requests, "Random 100", null);
        assertEquals(100, result.size(), "Random 100 should return 100 requests");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSampleSizeLargerThanDataset() throws Exception {
        List<HttpRequestData> requests = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            HttpRequestData req = new HttpRequestData();
            req.setPayload("payload-" + i);
            requests.add(req);
        }

        Method method = DatasetTestRunnerService.class.getDeclaredMethod("sampleRequests", List.class, String.class, Long.class);
        method.setAccessible(true);

        List<HttpRequestData> result = (List<HttpRequestData>) method.invoke(datasetTestRunnerService, requests, "Random 100", 42L);
        assertEquals(50, result.size(), "Should return all 50 requests when sample size is 100");
    }
}
