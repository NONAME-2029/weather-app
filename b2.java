import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.InstantSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


public class b2 extends JFrame {
    private static final String API_KEY = "d7ec9ff81a91c3c42fdc3e9e38c02af4";  // Replace with your OpenWeatherMap API key
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather?q=";

   
    private InputPanel inputPanel;
    private TemperaturePanel temperaturePanel;
    private DescriptionPanel descriptionPanel;
    private HumidityWindPanel humidityWindPanel;
    private Color startColor;
    private Color endColor;
    private float transitionProgress = 0.0f;
    private Timer colorTimer;
    private Timer transitionTimer;

    public b2() {
        setTitle("Weather App");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Timer to change gradient colors every 5 seconds
        colorTimer = new Timer(5000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                generateRandomColors();
                transitionProgress = 0.0f; // Reset progress for the next transition
            }
        });
        colorTimer.start();

        // Timer to smoothly transition the gradient with a progressive diagonal effect
        transitionTimer = new Timer(30, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Only update progress if it's less than or equal to 1.0 (full transition)
                if (transitionProgress < 1.0f) {
                    transitionProgress += 0.01f; // Smoother, slower progression
                    repaint();
                }
            }
        });
        transitionTimer.start();

        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // Interpolating colors for smooth transition
                Color currentColor1 = interpolateColor(startColor, endColor, transitionProgress);
                Color currentColor2 = interpolateColor(endColor, startColor, transitionProgress);

                // Creating a gradient that starts transitioning progressively from bottom-left to top-right
                int width = getWidth();
                int height = getHeight();

                float progressX = transitionProgress * width;   // Progression from left to right
                float progressY = transitionProgress * height;  // Progression from bottom to top

                GradientPaint gp = new GradientPaint(0, height, currentColor1, progressX, height - progressY, currentColor2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, width, height);
            }
        };
        contentPanel.setLayout(new GridBagLayout());
        setContentPane(contentPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // Input Panel
        inputPanel = new InputPanel(e -> handleGetWeather());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        contentPanel.add(inputPanel, gbc);

        // Temperature Panel
        temperaturePanel = new TemperaturePanel();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        contentPanel.add(temperaturePanel, gbc);

        // Description Panel
        descriptionPanel = new DescriptionPanel();
        gbc.gridx = 1;
        contentPanel.add(descriptionPanel, gbc);

        // Humidity/Wind Panel
        humidityWindPanel = new HumidityWindPanel();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        contentPanel.add(humidityWindPanel, gbc);

        // Initialize random colors
        generateRandomColors();
    }

  private void generateRandomColors() {
    // Generate start color
    startColor = new Color((int) (Math.random() * 256), 
                           (int) (Math.random() * 256), 
                           (int) (Math.random() * 256));
    
    // Generate a different end color until it's distinct from startColor
    do {
        endColor = new Color((int) (Math.random() * 256), 
                             (int) (Math.random() * 256), 
                             (int) (Math.random() * 256));
    } while (endColor.getRed() == startColor.getRed() &&
             endColor.getGreen() == startColor.getGreen() &&
             endColor.getBlue() == startColor.getBlue());
}

    private Color interpolateColor(Color c1, Color c2, float fraction) {
        int red = (int) (c1.getRed() + fraction * (c2.getRed() - c1.getRed()));
        int green = (int) (c1.getGreen() + fraction * (c2.getGreen() - c1.getGreen()));
        int blue = (int) (c1.getBlue() + fraction * (c2.getBlue() - c1.getBlue()));
        return new Color(red, green, blue);
    }
    private void handleGetWeather() {
        String city = inputPanel.getCityName();
        if (!city.isEmpty()) {
            fetchWeatherData(city);
        } else {
            JOptionPane.showMessageDialog(this, "Please enter a city name.");
        }
    }

    private void fetchWeatherData(String city) {
        try {
            String urlString = BASE_URL + city + "&appid=" + API_KEY + "&units=metric";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
    
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {  // Success
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
    
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
    
                in.close();
                conn.disconnect();
    
                // Parse weather data manually from JSON response
                String weatherData = content.toString();
                double temperature = extractTemperature(weatherData);
                String description = extractDescription(weatherData);
                double humidity = extractHumidity(weatherData);
                double windSpeed = extractWindSpeed(weatherData);
                String iconCode = extractIconCode(weatherData);
    
                // Fetch the weather icon using the icon code from OpenWeatherMap
                ImageIcon weatherIcon = new ImageIcon(new URL("http://openweathermap.org/img/wn/" + iconCode + "@2x.png"));
    
                // Update the UI
                updateWeatherUI(temperature, humidity, windSpeed, description, weatherIcon);
            } else {
                JOptionPane.showMessageDialog(this, "City not found or invalid API key.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching weather data.");
        }
    }
    
    // Corrected extractDescription method (no changes needed here, it's fine)
    private String extractDescription(String weatherData) {
        String descKeyword = "\"description\":\"";
        int descIndex = weatherData.indexOf(descKeyword);
        if (descIndex != -1) {
            return weatherData.substring(descIndex + descKeyword.length(), weatherData.indexOf("\"", descIndex + descKeyword.length()));
        }
        return "N/A";
    }
    
    // Update UI method now expects an ImageIcon


    // Helper method to extract temperature
    private double extractTemperature(String weatherData) {
        String tempKeyword = "\"temp\":";
        int tempIndex = weatherData.indexOf(tempKeyword);
        if (tempIndex != -1) {
            String tempString = weatherData.substring(tempIndex + tempKeyword.length(), weatherData.indexOf(",", tempIndex));
            return Double.parseDouble(tempString);
        }
        return 0;
    }

    // Helper method to extract weather description

    // Helper method to extract humidity
    private double extractHumidity(String weatherData) {
        String humidityKeyword = "\"humidity\":";
        int humidityIndex = weatherData.indexOf(humidityKeyword);
        if (humidityIndex != -1) {
            String humidityString = weatherData.substring(humidityIndex + humidityKeyword.length(), weatherData.indexOf(",", humidityIndex));
            return Double.parseDouble(humidityString);
        }
        return 0;
    }

    // Helper method to extract wind speed
    private double extractWindSpeed(String weatherData) {
        String windSpeedKeyword = "\"speed\":";
        int windSpeedIndex = weatherData.indexOf(windSpeedKeyword);
        if (windSpeedIndex != -1) {
            String windSpeedString = weatherData.substring(windSpeedIndex + windSpeedKeyword.length(), weatherData.indexOf(",", windSpeedIndex));
            return Double.parseDouble(windSpeedString);
        }
        return 0;
    }

    // Helper method to extract icon code
    private String extractIconCode(String weatherData) {
        String iconKeyword = "\"icon\":\"";
        int iconIndex = weatherData.indexOf(iconKeyword);
        if (iconIndex != -1) {
            return weatherData.substring(iconIndex + iconKeyword.length(), weatherData.indexOf("\"", iconIndex + iconKeyword.length()));
        }
        return "01d"; // Default clear sky icon
    }

  // In b2 class
private void updateWeatherUI(double temperature, double humidity, double windSpeed, String description, ImageIcon weatherIcon) {
    // Fade out all components before updating
   
    
    // After fade out, update components
    SwingUtilities.invokeLater(() -> {
        temperaturePanel.updateTemperature(String.valueOf(temperature) + " °C", new ImageIcon(new ImageIcon("thermometer.png").getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH)));
        descriptionPanel.updateDescription(description, weatherIcon);
        humidityWindPanel.updateHumidityWind(String.valueOf(humidity) + " %", String.valueOf(windSpeed) + " m/s");

        // Fade in the updated components
        
    });
}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new b2().setVisible(true));    }
}










public class InputPanel extends JPanel {
    private JTextField cityField;
    private JButton getWeatherButton;

    public InputPanel(ActionListener actionListener) {
        setOpaque(false);
        setLayout(new FlowLayout());

        JLabel cityLabel = new JLabel("Enter City: ");
        cityLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        cityLabel.setForeground(Color.WHITE);

        cityField = new JTextField(15);
        cityField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        cityField.setPreferredSize(new Dimension(200, 30));

        getWeatherButton = new JButton("Get Weather");
        getWeatherButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        getWeatherButton.setForeground(Color.WHITE);
        getWeatherButton.setBackground(new Color(30, 144, 255));
        getWeatherButton.addActionListener(actionListener);

        add(cityLabel);
        add(cityField);
        add(getWeatherButton);
    }

    public String getCityName() {
        return cityField.getText().trim();
    }
}



public class TemperaturePanel extends JPanel {
    private JLabel temperatureLabel;
    private JLabel dayLabel;
    private JLabel thermometerIconLabel;

    public TemperaturePanel() {
        setPreferredSize(new Dimension(250, 180));
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(255, 255, 255, 120));
        setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 80), 3, true));

        JLabel tempTitle = new JLabel("Temperature", SwingConstants.CENTER);
        tempTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        tempTitle.setForeground(Color.WHITE);
        tempTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(tempTitle);

        dayLabel = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE")), SwingConstants.CENTER);
        dayLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        dayLabel.setForeground(Color.WHITE);
        dayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(dayLabel);

        temperatureLabel = new JLabel("Temperature: ", SwingConstants.CENTER);
        temperatureLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        temperatureLabel.setForeground(Color.WHITE);
        temperatureLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(temperatureLabel);

        thermometerIconLabel = new JLabel();
        thermometerIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(thermometerIconLabel);
    }

    public void updateTemperature(String temp, ImageIcon icon) {
        temperatureLabel.setText("Temperature: " + temp);
        thermometerIconLabel.setIcon(icon);
    }
}


public class DescriptionPanel extends JPanel {
    private JLabel weatherDescriptionLabel;
    private JLabel weatherIconLabel;

    public DescriptionPanel() {
        setPreferredSize(new Dimension(250, 180));
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(255, 255, 255, 120));
        setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 80), 2, true));

        weatherIconLabel = new JLabel();
        weatherIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(weatherIconLabel);

        weatherDescriptionLabel = new JLabel("Description: ", SwingConstants.CENTER);
        weatherDescriptionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        weatherDescriptionLabel.setForeground(Color.WHITE);
        weatherDescriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(weatherDescriptionLabel);
    }

    public void updateDescription(String description, ImageIcon icon) {
        weatherDescriptionLabel.setText(description);
        weatherIconLabel.setIcon(icon);
    }
}


public class HumidityWindPanel extends JPanel {
    private JLabel humidityLabel;
    private JLabel windSpeedLabel;

    public HumidityWindPanel() {
        setPreferredSize(new Dimension(350, 250));
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(255, 255, 255, 140));
        setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 80), 2, true));

        JLabel humidityIconLabel = new JLabel(new ImageIcon(new ImageIcon("humidity.png").getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH)));
        humidityIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(humidityIconLabel);

        humidityLabel = new JLabel("Humidity: ", SwingConstants.CENTER);
        humidityLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        humidityLabel.setForeground(Color.WHITE);
        humidityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(humidityLabel);

        JLabel windSpeedIconLabel = new JLabel(new ImageIcon(new ImageIcon("storm.png").getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH)));
        windSpeedIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(windSpeedIconLabel);

        windSpeedLabel = new JLabel("Wind Speed: ", SwingConstants.CENTER);
        windSpeedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        windSpeedLabel.setForeground(Color.WHITE);
        windSpeedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(windSpeedLabel);
    }

    public void updateHumidityWind(String humidity, String windSpeed) {
        humidityLabel.setText("Humidity: " + humidity);
        windSpeedLabel.setText("Wind Speed: " + windSpeed);
    }
}










