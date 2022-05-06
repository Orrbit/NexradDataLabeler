# Nexrad Data Labeller
This is a current work in progress. Current state of the repository is that it processes the list of storm events and cross references the noaa-nexrad-level2 bucket to see the closest file to the start date of the storm. 

Since the files of nexrad follow a naming convention determined by the date and the Station ID, the application parses each storm event and composes the file path which the object should be found. It then loops through every s3 object key to see the one which indicates it is closest to the start time (hours and minutes) of the storm.

## Input
Storm Events + Closest Station
 - Begin Date & Timezone
 - Begin Latitude & Longitude
 - Event Type
 - Event ID
 - Station ID

## Output

Log messages for each storm event

Storm Event + Nexrad Level II File Object Path of storm

