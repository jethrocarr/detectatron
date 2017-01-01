package detectatron;

import com.amazonaws.util.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;


import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import javax.validation.ValidationException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Scanner;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;


/**
 * Unit tests for the Scanner Controller.
 */
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
public class ScannerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Fetch the specified file from the application resources and return the raw data.
     * @param filename
     * @return
     */
    private byte[] getResourceFile(String filename) {

        try {
            ClassLoader classLoader = getClass().getClassLoader();
            return IOUtils.toByteArray(classLoader.getResourceAsStream(filename));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Test the simple endpoint for /scanner.
     *
     * @throws Exception
     */
    @Test
    public void testScanner() throws Exception {
        mockMvc.perform(get("/scanner"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string("This endpoint must be invoked via POST with an image file\n"));
    }

    /**
     * Test posting to the image scanner without any image data. As it's the client's fault, we expect a 400-series
     * error.
     *
     * @throws Exception
     */
    @Test
    public void testScannerImageInvalidRequest() throws Exception {
        mockMvc.perform(fileUpload("/scanner/image"))
                .andExpect(status().is4xxClientError());
    }


    /**
     * Test posting to the image scanner with an invalid file format (plain text)
     *
     * @throws Exception
     */
    @Test
    public void testScannerImageInvalidFormat() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "test.txt", "text/plain", "Unit Testing".getBytes());

        mockMvc.perform(fileUpload("/scanner/image").file(multipartFile))
                .andExpect(status().is5xxServerError());
    }


    /**
     * Test posting to the image scanner with the traditional image of Lena
     *
     * @throws Exception
     */
    @Test
    public void testScannerImageLena1() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "lena1.png", "image/png", getResourceFile("images/lena1.png"));

        mockMvc.perform(fileUpload("/scanner/image").file(multipartFile))
                .andExpect(status().is2xxSuccessful());
    }


}