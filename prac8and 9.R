# --- Step 1: Install and Load Required Libraries ---
# Run these lines if you don't have the packages installed
# install.packages("tidyverse")
# install.packages("lubridate")
# install.packages("viridis")

# Load the libraries
library(tidyverse) # For data manipulation (dplyr) and plotting (ggplot2)
library(lubridate) # For handling dates and times
library(viridis)   # For colorblind-friendly color palettes

# --- Step 2: Load and Clean the Dataset ---

# Load the dataset
# IMPORTANT: Make sure this CSV file is in your R working directory
upi_data <- read.csv("/Users/madhurshinde/Downloads/upi_transactions_2024.csv")

# Convert timestamp to a proper date-time format
upi_data$timestamp <- ymd_hms(upi_data$timestamp)

# Rename columns to remove spaces and parentheses for easier use
colnames(upi_data) <- c("transaction_id", "timestamp", "transaction_type", "merchant_category",
                        "amount_inr", "transaction_status", "sender_age_group",
                        "receiver_age_group", "sender_state", "sender_bank",
                        "receiver_bank", "device_type", "network_type", "fraud_flag",
                        "hour_of_day", "day_of_week", "is_weekend")

# Convert categorical variables to factors for better plotting
categorical_cols <- c("transaction_type", "merchant_category", "transaction_status",
                      "sender_age_group", "receiver_age_group", "sender_state",
                      "sender_bank", "receiver_bank", "device_type", "network_type",
                      "day_of_week", "is_weekend", "fraud_flag")
upi_data <- upi_data %>%
  mutate(across(all_of(categorical_cols), as.factor))

# Display the structure of the data to confirm changes
str(upi_data)

# --- Step 3: Top 6 Most Useful Analyses ---

# Analysis 1: How are transaction amounts distributed?
# A log scale is used because financial data is often skewed (many small transactions)
ggplot(upi_data, aes(x = amount_inr)) +
  geom_histogram(bins = 50, fill = "skyblue", color = "black") +
  scale_x_log10() +
  labs(title = "Distribution of Transaction Amounts (Log Scale)",
       x = "Amount in INR (Log Scale)", y = "Frequency") +
  theme_minimal()

# Analysis 2: What are the most common transaction types?
ggplot(upi_data, aes(x = fct_infreq(transaction_type), fill = transaction_type)) +
  geom_bar() +
  labs(title = "Frequency of Transaction Types", x = "Transaction Type", y = "Count") +
  theme_minimal()

# Analysis 3: When are transactions most frequent? (Heatmap)
# This helps to find unusual activity at odd hours.
upi_data %>%
  group_by(day_of_week, hour_of_day) %>%
  summarise(count = n(), .groups = 'drop') %>%
  ggplot(aes(x = hour_of_day, y = factor(day_of_week, levels=c("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")), fill = count)) +
  geom_tile(color = "white") +
  scale_fill_viridis(option = "C") +
  labs(title = "Transaction Heatmap by Day and Hour",
       x = "Hour of Day", y = "Day of the Week")

# Analysis 4: Do fraud amounts look different from normal amounts?
# This is key for fraud detection.
ggplot(upi_data, aes(x = amount_inr, fill = fraud_flag)) +
  geom_density(alpha = 0.6) +
  scale_x_log10() +
  labs(title = "Transaction Amount Distribution for Fraud vs. Non-Fraud",
       x = "Amount (Log Scale)", y = "Density") +
  scale_fill_manual(values = c("0" = "green", "1" = "red"))

# Analysis 5: Is fraud more common on certain devices?
ggplot(upi_data, aes(x = device_type, fill = fraud_flag)) +
  geom_bar(position = "dodge") +
  labs(title = "Fraudulent Transactions by Device Type",
       x = "Device Type", y = "Count") +
  scale_fill_manual(values = c("0" = "gray", "1" = "red"))

# Analysis 6: Are there high-risk times and amounts?
# This plot combines hour, amount, and fraud status in one view.
ggplot(upi_data, aes(x = hour_of_day, y = amount_inr, color = fraud_flag)) +
  geom_point(alpha = 0.3) +
  scale_y_log10() +
  scale_color_manual(values = c("0" = "black", "1" = "red")) +
  labs(title = "Transactions by Hour and Amount, Highlighting Fraud",
       x = "Hour of Day", y = "Amount (Log Scale)")


# Analysis 7: Which states send the most transactions?
# This shows geographic hotspots for activity.
upi_data %>%
  count(sender_state, sort = TRUE) %>%
  top_n(10) %>%
  ggplot(aes(x = reorder(sender_state, n), y = n, fill = sender_state)) +
  geom_bar(stat = "identity") +
  coord_flip() +
  labs(title = "Top 10 States by Transaction Volume", x = "State", y = "Number of Transactions") +
  theme_minimal() +
  guides(fill = "none")

# Analysis 8: How do amounts differ by transaction type?
# Boxplots show the median and spread (interquartile range) of amounts.
ggplot(upi_data, aes(x = transaction_type, y = amount_inr, fill = transaction_type)) +
  geom_boxplot() +
  scale_y_log10() +
  labs(title = "Transaction Amount Distribution by Type",
       x = "Transaction Type", y = "Amount in INR (Log Scale)") +
  theme_minimal()

# Analysis 9: What are the top merchant categories, and what are their amounts?
# This helps identify which business types see the most activity and their typical transaction values.
top_categories <- upi_data %>% count(merchant_category, sort = TRUE) %>% top_n(10)
upi_data %>%
  filter(merchant_category %in% top_categories$merchant_category) %>%
  ggplot(aes(x = reorder(merchant_category, -amount_inr, FUN = median), y = amount_inr, fill = merchant_category)) +
  geom_boxplot() +
  scale_y_log10() +
  theme(axis.text.x = element_text(angle = 45, hjust = 1)) +
  guides(fill="none") +
  labs(title = "Transaction Amount by Top 10 Merchant Categories", x = "Merchant Category", y = "Amount (Log Scale)")