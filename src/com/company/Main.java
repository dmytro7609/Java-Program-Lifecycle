package com.company;

import java.awt.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main {

    String processName = "programName.exe";

    private static final String TASKLIST = "tasklist"; // prikaz pouzity na ziskanie zoznamu "tasklist"
    private static final String KILL = "taskkill /F /IM "; // prikaz pouzity na odstranenie ulohy

    public static int xFirst; // Prva X suradnica umiestnenia mysi
    public static int yFirst; // Prva Y suradnica umiestnenia mysi
    public static int xSecond; // Druha X suradnica umiestnenia mysi
    public static int ySecond; // Druha Y suradnica umiestnenia mysi

    StringWriter errors = new StringWriter();

    public void createLogFile() {
        try {
            File logfile = new File("logfile.txt");
            if (logfile.createNewFile()) {
                writeLogFile("New logfile created");
            }
        } catch (IOException e) {
            e.printStackTrace(new PrintWriter(errors));
            writeLogFile((errors.toString().replaceAll("\t|\n|\r\n", " ")));
        }
    }

    public String getCurrentTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }

    public void writeLogFile(String value) {
        String timenow = getCurrentTime();
        try {
            createLogFile();
            FileWriter myWriter = new FileWriter("logfile.txt", true);
            myWriter.write(timenow + " | " + value + "\n");
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace(new PrintWriter(errors));
            writeLogFile((errors.toString().replaceAll("\t|\n|\r\n", " ")));
        }
    }

    public boolean isProcessRunning(String serviceName) {

        try {
            Process pro = Runtime.getRuntime().exec(TASKLIST);
            BufferedReader reader = new BufferedReader(new InputStreamReader(pro.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(serviceName)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace(new PrintWriter(errors));
            writeLogFile((errors.toString().replaceAll("\t|\n|\r\n", " ")));
        }
        return false;
    }

    public void killProcess(String serviceName) {
        try {
            Runtime.getRuntime().exec(KILL + serviceName);
            writeLogFile(serviceName + " uspesne zmazany!");
            reStart(1);
        } catch (IOException e) {
            e.printStackTrace(new PrintWriter(errors));
            writeLogFile((errors.toString().replaceAll("\t|\n|\r\n", " ")));
        }
    }

    public void reStart(int value) {
        switch (value) {
            case 1:
                getTask(60, 1);
                break;
            case 2:
                getTask(60, 2);
                break;
            default:
                break;
        }
    }

    public void getMouseLocation(int count) {
        if (count == 1) {
            xFirst = MouseInfo.getPointerInfo().getLocation().x;
            yFirst = MouseInfo.getPointerInfo().getLocation().y;
            reStart(2);
        } else if (count == 2) {
            xSecond = MouseInfo.getPointerInfo().getLocation().x;
            ySecond = MouseInfo.getPointerInfo().getLocation().y;
            if ((xFirst == xSecond) && (yFirst == ySecond)) {
                killProcess(processName);
            } else {
                writeLogFile("Pouzivatel je aktivny");
                reStart(1);
            }
        }
    }

    public void getTask(int minute, int value) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                switch (value) {
                    case 1:
                        try {
                            boolean isRunning = isProcessRunning(processName);
                            writeLogFile("is " + processName + " running : " + isRunning);
                            if (isRunning) {
                                writeLogFile("Proces existuje, podme dalej");
                                getMouseLocation(1);
                            } else {
                                writeLogFile("Nepodarilo sa najst proces : " + processName);
                                reStart(1);
                            }
                            getTimeLogCount();
                        } catch (Exception e) {
                            e.printStackTrace(new PrintWriter(errors));
                            writeLogFile((errors.toString().replaceAll("\t|\n|\r\n", " ")));
                        }
                        break;
                    case 2:
                        getMouseLocation(2);
                        break;
                }
            }
        }, minute * 60 * 1000);
    }


    public void getTimeLogCount() {
        String firstLine = "";
        String lastLine = "";
        int countLine = 0;
        int firstCount = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader("logfile.txt"));
            try {
                String line = br.readLine();
                while (line != null) {
                    countLine++;
                    if (firstCount == 0) {
                        firstCount += 1;
                        firstLine = line;
                    }
                    lastLine = line;
                    line = br.readLine();
                }
            } finally {
                br.close();
            }
        } catch (Exception e) {
            e.printStackTrace(new PrintWriter(errors));
            writeLogFile((errors.toString().replaceAll("\t|\n|\r\n", " ")));
        }
        if (countLine > 2) {
            String pattern = "^(\\d\\d)\\W(\\d\\d)\\W(\\d\\d\\d\\d)\\s(\\d\\d\\W\\d\\d\\W\\d\\d)";

            Pattern r = Pattern.compile(pattern);
            Matcher fisttime = r.matcher(firstLine);
            Matcher lasttime = r.matcher(lastLine);
            if (fisttime.find()) {
                firstLine = fisttime.group(0);
            }
            if (lasttime.find()) {
                lastLine = lasttime.group(0);
            }

            try {
                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                Date date1 = dateFormat.parse(firstLine);
                Date date2 = dateFormat.parse(lastLine);
                long unixTime1 = date1.getTime() / 1000;
                long unixTime2 = date2.getTime() / 1000;
                long differenceTime = ((unixTime2 - unixTime1) / 60);
                if (differenceTime >= 5) {
                    new PrintWriter("logfile.txt").close();
                }
            } catch (Exception e) {
                e.printStackTrace(new PrintWriter(errors));
                writeLogFile((errors.toString().replaceAll("\t|\n|\r\n", " ")));
            }
        }
    }

    public static void main(String[] args) {
        new Main().getTask(1, 1);
    }
}