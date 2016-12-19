package name.gyger.jmoney.report;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class ReportControllerTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockMvc.perform(put("/rest/options/init"));
    }

    @Test
    public void testGetBalances() throws Exception {
        mockMvc.perform(get("/rest/reports/balances"))
                .andExpect(content().json("[{'accountName':'Gesamt','balance':0,'total':true}]"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetCashFlows() throws Exception {
        mockMvc.perform(get("/rest/reports/cash-flows")
                .param("fromDate", "2016-12-01")
                .param("toDate", "2016-12-31"))
                .andExpect(content().json("[{'categoryId':null,'categoryName':'Gesamttotal','income':0,'expense':0,'difference':0,'total':true}]"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetInconsistentSplitEntries() throws Exception {
        mockMvc.perform(get("/rest/reports/consitency/inconsistent-split-entries"))
                .andExpect(content().json("[]"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetEntriesWithoutCategory() throws Exception {
        mockMvc.perform(get("/rest/reports/consitency/entries-without-category"))
                .andExpect(content().json("[]"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetEntriesWithCategory() throws Exception {
        mockMvc.perform(get("/rest/reports/entries-with-category")
                .param("categoryId", "0")
                .param("fromDate", "2016-12-01")
                .param("toDate", "2016-12-31"))
                .andExpect(content().json("[]"))
                .andExpect(status().isOk());
    }

}
