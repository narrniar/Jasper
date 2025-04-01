package com.example.jaspertable.service;

import jakarta.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class JReportService {

    private static final String REPORT_TEMPLATE = "reports/template.jrxml";
    private static final String ANKETA_TEMPLATE = "reports/anketa/anketa.jrxml";
    private static final String SUBREPORT_TEMPLATE = "reports/subreports/relative.jrxml";
    private static final String SUBREPORT_MASTER_TEMPLATE = "reports/subreports/relatives.jrxml";
    private static final String RESULTS_TEMPLATE = "reports/results/results.jrxml";

    public void exportJasperReport(Map<String, Object> data, HttpServletResponse response) {
        try {
            // Wrap data into a collection
            List<Map<String, ?>> dataList = Collections.singletonList(data);
            JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(dataList);

            // ✅ Load JRXML file correctly from resources
            InputStream reportStream = getClass().getClassLoader().getResourceAsStream(REPORT_TEMPLATE);
            if (reportStream == null) {
                throw new JRException("❌ Report template not found: " + REPORT_TEMPLATE);
            }

            // Compile the report
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);


            // Fill the report with data
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, data, dataSource);


            // Set response headers
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=Report.pdf");

            // Export PDF
            JasperExportManager.exportReportToPdfStream(jasperPrint, response.getOutputStream());

            response.getOutputStream().flush();
        } catch (Exception e) {
            e.printStackTrace();
            handleError(response, "Error generating report: " + e.getMessage(), e);
        }
    }

    public void generateAnketa(Map<String, Object> data, HttpServletResponse response) {
        try {
            // Wrap data into a collection
            List<Map<String, ?>> dataList = Collections.singletonList(data);
            JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(dataList);

            // ✅ Load JRXML file correctly from resources
            InputStream reportStream = getClass().getClassLoader().getResourceAsStream(ANKETA_TEMPLATE);
            if (reportStream == null) {
                throw new JRException("❌ Report template not found: " + ANKETA_TEMPLATE);
            }

            // Compile the report
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);


            // Fill the report with data
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, data, dataSource);


            // Set response headers
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=Anketa.pdf");

            // Export PDF
            JasperExportManager.exportReportToPdfStream(jasperPrint, response.getOutputStream());

            response.getOutputStream().flush();
        } catch (Exception e) {
            e.printStackTrace();
            handleError(response, "Error generating report: " + e.getMessage(), e);
        }
    }

    public void generateResultsReport(Map<String, ?> data, HttpServletResponse response) {
        try {
            if (data == null || data.isEmpty()) {
                throw new IllegalArgumentException("Data list cannot be empty");
            }
            List<Map<String, ?>> dataList = Collections.singletonList(data);
            JRMapCollectionDataSource dataSourceMain = new JRMapCollectionDataSource(dataList);

            InputStream mainReportStream = getClass().getClassLoader().getResourceAsStream(RESULTS_TEMPLATE);
            InputStream subReportStream = getClass().getClassLoader().getResourceAsStream(SUBREPORT_TEMPLATE);
            InputStream subReportMasterStream = getClass().getClassLoader().getResourceAsStream(SUBREPORT_MASTER_TEMPLATE);

            if (mainReportStream == null) {
                throw new JRException("Master report template not found: " + SUBREPORT_MASTER_TEMPLATE);
            }
            if (subReportStream == null) {
                throw new JRException("Subreport template not found: " + SUBREPORT_TEMPLATE);
            }
            // Compile reports
            JasperReport mainReport = JasperCompileManager.compileReport(mainReportStream);
            JasperReport subreport = JasperCompileManager.compileReport(subReportStream);
            JasperReport subreportMaster = JasperCompileManager.compileReport(subReportMasterStream);

            // Prepare lists for dynamic subreports
            List<JasperReport> subreportSources = new ArrayList<>();
            List<JRDataSource> subreportDataSources = new ArrayList<>();

            List<Map<String, ?>> relatives = (List<Map<String, ?>>) data.get("relatives");
            for (Map<String, ?> item : relatives) {
                subreportSources.add(subreport);
                JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(Collections.singletonList(item));
                subreportDataSources.add(dataSource);
            }

            Map<String, Object> parameters = new HashMap<>(data);
            parameters.put("SubreportSources", subreportSources);
            parameters.put("SubreportDataSources", subreportDataSources);
            // Fill the report with data
            JasperPrint jasperPrint = JasperFillManager.fillReport(mainReport, (Map<String, Object>) data, dataSourceMain);

            JRDataSource mainDataSource = new JREmptyDataSource(subreportSources.size());



// Fill second report
            JasperPrint jasperPrint2 = JasperFillManager.fillReport(subreportMaster, parameters, mainDataSource );
            jasperPrint.getPages().addAll(jasperPrint2.getPages());
            // Set response headers
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=Results.pdf");
            // Export PDF
            JasperExportManager.exportReportToPdfStream(jasperPrint, response.getOutputStream());

            response.getOutputStream().flush();
        } catch (Exception e) {
            e.printStackTrace();
            handleError(response, "Error generating report: " + e.getMessage(), e);
        }
    }

    private void handleError(HttpServletResponse response, String message, Exception e) {
        try {
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
            }
        } catch (Exception ioException) {
            ioException.printStackTrace();
        }
        e.printStackTrace();
    }



    public static JasperReport compileReport(String jrxmlPath) {
        try {
            // Compile the JRXML file and return the compiled JasperReport object
            return JasperCompileManager.compileReport(jrxmlPath);
        } catch (JRException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to compile JRXML file: " + jrxmlPath, e);
        }
    }


}
