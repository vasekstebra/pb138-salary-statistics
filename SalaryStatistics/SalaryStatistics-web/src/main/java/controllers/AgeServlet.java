package controllers;

import com.google.gson.Gson;
import dao.Age;
import dao.AgeManager;
import dao.AgeManagerImpl;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.dbcp2.BasicDataSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Vladimír Jarabica
 */
@WebServlet(urlPatterns = {"/age/*"})
public class AgeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        DataSource source = (DataSource) getServletContext().getAttribute("dataSource");
        AgeManagerImpl manager = new AgeManagerImpl();
        manager.setDataSource(source);
        String action = request.getPathInfo();
        if (action == null) {
            action = "/";
        }
        switch (action) {
            case "/": {
                try {
                    Document options = getOptions(manager, request, request.getQueryString() == null);
                    Document tableData = getData(manager, request, request.getQueryString() != null);
                    request.setAttribute("options", documentToString(options));
                    request.setAttribute("table", documentToString(tableData));
                    request.setAttribute("graphUrl", "age");
                    request.getRequestDispatcher("/template.jsp").forward(request, response);
                }catch (ParserConfigurationException | TransformerException | TransformerFactoryConfigurationError | IllegalArgumentException ex) {
                    Logger.getLogger(SectorServlet.class.getName()).log(Level.SEVERE, null, ex);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
            case "/data": {
                String data = getJsonData(manager, request);
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json");
                response.getWriter().write(data);
             }
             break;
        }
    }

    private Document getData(AgeManager manager, HttpServletRequest request, boolean filter) throws IOException, ParserConfigurationException {
        List<Age> ages = manager.findAllAges();
        if(filter) {
            String[] years = request.getParameterValues("year");
            String[] intervals = request.getParameterValues("interval");
            String[] countries = request.getParameterValues("country");
            ages = filterByCountry(ages, countries);
            ages = filterByYear(ages, years);
            ages = filterByInterval(ages, intervals);
        }
        return returnTableData(ages);
    }

    private Document getOptions(AgeManager manager, HttpServletRequest request, boolean checkAll) throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = docFactory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element rootElement = doc.createElement("form");
        rootElement.setAttribute("method", "GET");
        rootElement.setAttribute("action", request.getContextPath() + "/age");
        
        Element div = doc.createElement("div");
        div.setAttribute("class", "form-group");

        Set<String> years = new HashSet<>();
        Set<String> ageIntervals = new HashSet<>();
        Set<String> countries = new HashSet<>();
        List<Age> ages = manager.findAllAges();
        for (Age age : ages) {
            years.add(age.getYear());
            ageIntervals.add(convertToInterval(age));
            countries.add(age.getCountry());
        }

        String[] yearsParametersValues = request.getParameterValues("year");
        for (String year : years) {
            Element label = doc.createElement("label");
            label.setAttribute("class", "checkbox-inline");
            Element input = doc.createElement("input");
            input.setAttribute("type", "checkbox");
            input.setAttribute("name", "year");
            if (yearsParametersValues != null) {
                for (String parameterYear : yearsParametersValues) {
                    if (parameterYear.equals(year)) {
                        input.setAttribute("checked", "");
                    }
                }
            } else if (checkAll) {
                input.setAttribute("checked", "");
            }
            input.setAttribute("value", year);
            input.setTextContent(year);
            label.appendChild(input);
            div.appendChild(label);
        }

        div.appendChild(doc.createElement("br"));

        String[] intervalsParametersValues = request.getParameterValues("interval");
        for (String ageInterval : ageIntervals) {
            Element label = doc.createElement("label");
            label.setAttribute("class", "checkbox-inline");
            Element input = doc.createElement("input");
            input.setAttribute("type", "checkbox");
            input.setAttribute("name", "interval");
            if(intervalsParametersValues != null) {
                for(String parameterInterval : intervalsParametersValues) {
                    if(parameterInterval.equals(ageInterval)) {
                        input.setAttribute("checked", "");
                    }
                }
            }
            if(checkAll) {
                input.setAttribute("checked", "");
            }
            input.setAttribute("value", ageInterval);
            input.setTextContent(ageInterval);
            label.appendChild(input);
            div.appendChild(label);
        }
        
        div.appendChild(doc.createElement("br"));
        
        String[] countryParametersValues = request.getParameterValues("country");
        for (String country : countries) {
            Element label = doc.createElement("label");
            label.setAttribute("class", "checkbox-inline");
            Element input = doc.createElement("input");
            input.setAttribute("type", "checkbox");
            input.setAttribute("name", "country");
            if (countryParametersValues != null) {
                for (String countryParameter : countryParametersValues) {
                    if (countryParameter.equals(country)) {
                        input.setAttribute("checked", "");
                    }
                }
            } else if (checkAll) {
                input.setAttribute("checked", "");
            }
            input.setAttribute("value", country);
            input.setTextContent(country);
            label.appendChild(input);
            div.appendChild(label);
        }

        rootElement.appendChild(div);

        Element submit = doc.createElement("input");
        submit.setAttribute("type", "submit");
        submit.setAttribute("class", "btn btn-primary");
        submit.setAttribute("value", "Zobrazit");
        rootElement.appendChild(submit);

        doc.appendChild(rootElement);
        return doc;
    }

    private Document returnTableData(List<Age> data) throws IOException, ParserConfigurationException {
        SortedSet<String> years = new TreeSet<>();
        for (Age age : data) {
            years.add(age.getYear());
        }

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = docFactory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element rootElement = doc.createElement("table");

        Element thead = doc.createElement("thead");
        Element theadRow = doc.createElement("tr");
        Element theadRow2 = doc.createElement("tr");
        Element th = doc.createElement("th");
        th.setTextContent("Interval");
        th.setAttribute("rowspan", "2");
        theadRow.appendChild(th);
        for (String year : years) {
            th = doc.createElement("th");
            th.setTextContent(year);
            th.setAttribute("colspan", "2");
            theadRow.appendChild(th);
            
            th = doc.createElement("th");
            th.setTextContent("MuĹľi");
            theadRow2.appendChild(th);
            
            th = doc.createElement("th");
            th.setTextContent("Ĺ˝eny");
            theadRow2.appendChild(th);
        }
        thead.appendChild(theadRow);
        thead.appendChild(theadRow2);

        Element tbody = doc.createElement("tbody");
        Map<String, List<Age>> ages = new HashMap<>();
        for (Age age : data) {
            if (ages.containsKey(convertToInterval(age))) {
                ages.get(convertToInterval(age)).add(age);
            } else {
                List<Age> a = new ArrayList<>();
                a.add(age);
                ages.put(convertToInterval(age), a);
            }
        }
        for (Entry<String, List<Age>> age : ages.entrySet()) {
            Element tr = doc.createElement("tr");
            Element td = doc.createElement("td");
            td.setTextContent(age.getKey());
            tr.appendChild(td);
            List<Age> values = age.getValue();
            values.sort(new Comparator<Age>() {

                @Override
                public int compare(Age o1, Age o2) {
                    return o1.getAgeFrom().compareTo(o2.getAgeFrom());
                }

            });
            for (Age a : values) {
                td = doc.createElement("td");
                td.setTextContent(a.getAverageSalary().toString());
                tr.appendChild(td);
            }
            tbody.appendChild(tr);
        }

        rootElement.appendChild(thead).appendChild(tbody);
        rootElement.setAttribute("class", "table");
        doc.appendChild(rootElement);
        return doc;
    }

    /*private void writeDocumentToResponse(Document doc, HttpServletResponse response) throws TransformerFactoryConfigurationError {
        try {
            String dataToWrite = documentToString(doc);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(dataToWrite);
        } catch (IllegalArgumentException | TransformerException | IOException ex) {
            Logger.getLogger(SectorServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }*/
    
    private String documentToString(Document doc) throws TransformerException, TransformerFactoryConfigurationError, TransformerConfigurationException, IllegalArgumentException {
        StringWriter sw = new StringWriter();
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(doc), new StreamResult(sw));
        String dataToWrite = sw.toString();
        return dataToWrite;
    }
    
    private String getJsonData(AgeManager manager, HttpServletRequest request) throws IOException {
        List<Age> data = manager.findAllAges();
        boolean filter = true;
        if (filter) {
            String[] years = request.getParameterValues("year");
            String[] intervals = request.getParameterValues("interval");
            String[] countries = request.getParameterValues("country");
            data = filterByYear(data, years);
            data = filterByInterval(data, intervals);
            data = filterByCountry(data, countries);
        }
        return new Gson().toJson(data);
    }

    private List<Age> filterByYear(List<Age> ages, String[] years) {
        List<Age> filtered = new ArrayList<>();
        if (years != null) {
            for (Age age : ages) {
                for (String year : years) {
                    if (age.getYear().equals(year)) {
                        filtered.add(age);
                    }
                }
            }
            return filtered;
        } else {
            return ages;
        }
    }

    private List<Age> filterByInterval(List<Age> ages, String[] intervals) {
        List<Age> filtered = new ArrayList<>();
        Set<String[]> intervalParts = new HashSet<>();
        if (intervals != null) {
            for (String interval : intervals) {
                System.out.println(interval);
                String[] parts = convertFromInterval(interval);
                intervalParts.add(parts);
                System.out.println(parts[0] + " - " + parts[1]);
            }
            for (Age age : ages) {
                for (String[] interval : intervalParts) {
                    if (age.getAgeFrom().toString().equals(interval[0]) && age.getAgeTo().toString().equals(interval[1])) {
                        filtered.add(age);
                    }
                }
            }
            return filtered;
        }
        return ages;
    }

    public static boolean isNumeric(String str) {
        try {
            double d = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private String convertToInterval(Age age) {
        if (age.getAgeFrom() == 0) {
            return "Do-" + age.getAgeTo().toString();
        }
        if (age.getAgeTo() == 100) {
            return age.getAgeFrom() + "-a vĂ­ce";
        }
        return age.getAgeFrom().toString() + "-" + age.getAgeTo().toString();
    }
    
    private String[] convertFromInterval(String interval) {
        String parts[] = interval.split("-", 2);
        if(parts[0].equals("Do")) {
            parts[0] = "0";
        } else if(parts[1].equals("a vĂ­ce")) {
            parts[1] = "100";
        }
        return parts;
    }
    
    private List<Age> filterByCountry(List<Age> ages, String[] countries) {
        List<Age> filtered = new ArrayList<>();
        if (countries != null) {
            for (Age age : ages) {
                for (String country : countries) {
                    if (age.getCountry().equals(country)) {
                        filtered.add(age);
                    }
                }
            }
            return filtered;
        } else {
            return filtered;
        }
    }
}