package com.olegchir.jug.site.parser.heisenbug2017moscowparser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;
import com.machinepublishers.jbrowserdriver.Timezone;
import org.apache.commons.io.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AppRunner implements CommandLineRunner {
    public String pUrl = "https://heisenbug-moscow.ru/";
    public String pWorkDir = "C:/temp/hb";

    @Override
    public void run(String... args) throws Exception {
        initParams();

        FileUtils.forceMkdir(new File(pWorkDir));
        FileUtils.forceMkdir(new File(getImageDirFileName()));

        List<Talk> talks;
        if (!cacheExists()) {
            System.out.println("Extracting talks from the Internets");
            talks = findTalks();
            for (Talk talk: talks) {
                fillDescription(talk, true);
            }
            saveCache(talks);
        } else {
            System.out.println("Extracting talks from cache");
            talks = loadCache();
        }

        applyReplacements(talks);
        applyStopList(talks);

        String result = renderTemplate(talks);
        saveResult(result);

        System.exit(0);
    }

    public void initParams() {
        String url = System.getProperty("url");
        if (null != url) {
            pUrl = url;
        } else {
            System.out.println("Using default URL");
        }

        String workdir = System.getProperty("workdir");
        if (null != workdir) {
            pWorkDir = workdir;
        } else {
            System.out.println("Using default working directory");
        }
    }

    public String getImageDirFileName() {
        return pWorkDir + File.separator + "img";
    }

    public boolean cacheExists() {
        return Files.exists(Paths.get(getCacheFileName()));
    }

    public void saveCache(List<Talk> talks) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        writer.writeValue(new File(getCacheFileName()), talks);
    }

    public List<Talk> loadCache() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<Talk> talks = mapper.readValue(new File(getCacheFileName()), new TypeReference<List<Talk>>(){});
        return talks;
    }

    public List<Talk> loadReplacements() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<Talk> talks = mapper.readValue(new File(getReplatecementsFileName()),
                new TypeReference<List<Talk>>(){});
        return talks;
    }

    public String getReplatecementsFileName() {
        return pWorkDir + File.separator + "replacements.json";
    }

    public void applyReplacements(List<Talk> cache) throws IOException {
        if (!Files.exists(Paths.get(getReplatecementsFileName()))) {
            return;
        }
        List<Talk> replacements = loadReplacements();
        for (Talk replacement: replacements) {
            for (Talk cacheItem: cache) {
                if (replacement.getId().equals(cacheItem.getId())) {
                    cacheItem.replaceWith(replacement);
                }
            }
        }
    }

    public String getStopListFileName() {
        return pWorkDir + File.separator + "stoplist.txt";
    }

    public void applyStopList(List<Talk> cache) throws IOException {
        if (!Files.exists(Paths.get(getStopListFileName()))) {
            return;
        }

        List<String> stoplist = Files.lines(Paths.get(getStopListFileName())).collect(Collectors.toList());
        for (Iterator<Talk> iterator = cache.iterator(); iterator.hasNext();) {
            Talk cacheItem = iterator.next();
            for (String stopword: stoplist) {
                if (cacheItem.getId().equals(stopword)) {
                    iterator.remove();
                }
            }
        }
    }

    public String getCacheFileName() {
        return pWorkDir + File.separator + "cache.json";
    }

    public void saveResult(String template) throws IOException {
        String filename = String.format("%s%s.md", pWorkDir + File.separator, "result");
        Path path = Paths.get(filename);
        Files.deleteIfExists(path);
        Files.write(path, template.getBytes());
    }

    public String getTemplateFileName() {
        return pWorkDir + File.separator + "template.vm";
    }

    public String renderTemplate(List<Talk> talks) throws IOException, ParseException {
        File tfile = new File(getTemplateFileName());
        String content = new String(Files.readAllBytes(tfile.toPath()));
         /*  first, get and initialize an engine  */
        VelocityEngine ve = new VelocityEngine();
        ve.init();
        /*  next, get the Template  */
        RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
        StringReader reader = new StringReader(content);
        SimpleNode node = runtimeServices.parse(reader, "Template");
        Template template = new Template();
        template.setRuntimeServices(runtimeServices);
        template.setData(node);
        template.initDocument();
        /*  create a context and add data */
        VelocityContext context = new VelocityContext();
        context.put("talks", talks);
        context.put("header",  getHeader());
        context.put("footer", getFooter());

        /* now render the template into a StringWriter */
        StringWriter writer = new StringWriter();
        template.merge( context, writer );
        /* show the World */
        return writer.toString();
    }

    public String getHeaderFileName() {
        return pWorkDir + File.separator + "header.md";
    }

    public String getHeader() throws IOException {
        List<String> content = Files.lines(Paths.get(getHeaderFileName())).collect(Collectors.toList());
        return content.stream().collect(Collectors.joining("\n"));
    }

    public String getFooterFileName() {
        return pWorkDir + File.separator + "footer.md";
    }

    public String getFooter() throws IOException {
        List<String> content = Files.lines(Paths.get(getFooterFileName())).collect(Collectors.toList());
        return content.stream().collect(Collectors.joining("\n"));
    }

    public List<Talk> findTalks() {
        List<Talk> talks = new ArrayList<>();

        JBrowserDriver rootDriver = new JBrowserDriver(Settings.builder().
                timezone(Timezone.AMERICA_NEWYORK).build());
        try {

            rootDriver.get(pUrl);
            List<WebElement> wtalks = rootDriver.findElementsByClassName("pt-col-talk");
            System.out.println("ok!");
            for (WebElement wtalk : wtalks) {
                WebElement element = null;
                try {
                    Talk talk = new Talk();
                    boolean talkIsValid = false;

                    //This line is intentional! Without this everything breaks, and I really don't know why.
                    //Without this line the next call to findElements will return an empty list, that is obviously an error.
                    WebElement speakerNamex = wtalk.findElement(By.className("pt-speaker_name"));

                    List<WebElement> speakerNames = wtalk.findElements(By.className("pt-speaker_name"));
                    for (WebElement speakerName: speakerNames) {
                        talkIsValid = true;
                        String text = speakerName.getText();
                        String[] split = text.split("\n");
                        if (split.length != 2) {
                            talkIsValid = false;
                            break;
                        }
                        int speakersNum = split.length/2;

                        for (int i=0; i<speakersNum; i++) {
                            Speaker speaker = new Speaker();
                            speaker.setSpeaker(split[i*2]);
                            speaker.setCompany(split[i*2 + 1]);
                            talk.getSpeakers().add(speaker);
                        }
                    }

                    if (!talkIsValid) {
                        continue;
                    }

                    element = wtalk.findElement(By.className("event_talk_link"));
                    String nameText = element.getText();
                    talk.setName(nameText);
                    talk.setId(nameText);

                    String href = element.getAttribute("href");
                    talk.setUrl(href);

                    talks.add(talk);
                    System.out.println(String.format("new talk (%s)", talk.getSpeakers().get(0).getSpeaker()));
                } catch (Exception e) {
                    // It's OK for this driver
                }
            }
        } finally {
            rootDriver.quit();
        }

        return talks;
    }

    public void fillDescription(Talk talk, boolean downloadImages) throws IOException {
        JBrowserDriver descDriver = new JBrowserDriver(Settings.builder().
                timezone(Timezone.AMERICA_NEWYORK).build());
        try {
            descDriver.get(talk.getUrl());
            WebElement element = descDriver.findElement(By.className("my-3"));
            List<WebElement> descParagraphs = element.findElements(By.xpath("./div/div/p"));
            List<String> descParagraphsAsString = new ArrayList<>();
            for (WebElement par : descParagraphs) {
                String parText = par.getText();
                descParagraphsAsString.add(parText);
            }
            talk.setDescription(descParagraphsAsString);

            for (int pos=2; pos <= talk.getSpeakers().size()*2; pos += 2) {
                Speaker speaker = talk.getSpeakers().get(pos / 2 - 1);
                String xpathPar = String.format("./div/div/div[contains(@class, 'row') and position()=%d]//p", pos);
                List<WebElement> bioParagraphs = element.findElements(By.xpath(xpathPar));
                for (WebElement par : bioParagraphs) {
                    String parText = par.getText();
                    speaker.getBio().add(parText);
                }

                String xpathImg = String.format("./div/div/div[contains(@class, 'row') and position()=%d]//img", pos);
                WebElement faceImg = element.findElement(By.xpath(xpathImg));
                String imgSrc = faceImg.getAttribute("src");
                speaker.setImageUrl(imgSrc);

                if (downloadImages) {
                    try (InputStream in = new URL(imgSrc).openStream()) {
                        String filename = String.format("%s%s.jpg", getImageDirFileName() + File.separator, speaker.getSpeaker());
                        Path path = Paths.get(filename);
                        Files.deleteIfExists(path);
                        Files.copy(in, path);
                    }
                }
            }
        } finally {
            descDriver.quit();
        }
    }
}
