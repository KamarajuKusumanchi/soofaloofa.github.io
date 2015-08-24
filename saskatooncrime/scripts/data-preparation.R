# Writes a CSV with counts for the number of crimes per year per neighbourhood.
library(gdata)
library(ggplot2)
library(dplyr)

# Load the data
filenames <- list.files("raw_data", pattern="*.xls", full.names=TRUE)
raw.dataset <- do.call(rbind, lapply(filenames, read.xls))

dataset <- raw.dataset

dataset$Year <- as.Date(paste(dataset$Year, "01", "01", sep="-"))  # Convert to Date objects requires month/day
names(dataset) <- c("Resource", "Year", "Neighbourhood", "Value", "Margin.of.Error")
dataset <- dataset[grepl("NBHD)$", dataset$Neighbourhood),]  # Only look at neighbourhoods

# Output summary statistics
# summary(dataset)
# str(dataset)

# Summarize by year
# Looks like crimes are dropping almost linearly.
# by_year <- group_by(dataset, Year)
# crimes.by_year <- summarise(by_year, Crimes = sum(Value))
# crimes.by_year
# summary(crimes.by_year)
# ggplot(crimes.by_year, aes(x = Year, y = Crimes)) + geom_point() + geom_smooth(method='lm')


# # Summarize by neighbourhood
# # Shows a wide discrepency between the most and the fewest crimes
# by_neighbourhood <- group_by(dataset, Neighbourhood)
# crimes.by_neighbourhood <- summarise(by_neighbourhood, Crimes = sum(Value))
# crimes.by_neighbourhood <- crimes.by_neighbourhood[with(crimes.by_neighbourhood, order(Crimes, decreasing=TRUE)),]
# summary(crimes.by_neighbourhood)
# ggplot(crimes.by_neighbourhood, aes(x = Neighbourhood, y = Crimes)) + geom_histogram(stat="identity")

# # Top 20 neighbourhoods by crime
# head(crimes.by_neighbourhood, n=20)

# # Bottom 20 neighbourhoods by crime
# tail(crimes.by_neighbourhood, n=20)


# Summarize by both year and neighbourhood
by_year.by_neighourhood <- group_by(dataset, Year, Neighbourhood)
crimes.by_year.by_neighourhood <- summarise(by_year.by_neighourhood, Crimes = sum(Value))
crimes.by_year.by_neighourhood <- crimes.by_year.by_neighourhood[with(crimes.by_year.by_neighourhood, order(Year)),]

write.csv(crimes.by_year.by_neighourhood, file="crimes-year-neighbourhood.csv", row.names=FALSE)
