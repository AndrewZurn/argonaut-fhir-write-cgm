# Dexcom CGM Data Overview

This README provides a summary of the key endpoints from the Dexcom Continuous Glucose Monitoring (CGM) sandbox API: `/egvs` and `/devices`, using the V3 version of the API.

## Table of Contents

- [Introduction](#introduction)
- [Endpoints](#endpoints)
  - [EGVs Endpoint](#egvs-endpoint)
  - [Devices Endpoint](#devices-endpoint)
- [Data Models](#data-models)
  - [EGV Data](#egv-data)
  - [Device Data](#device-data)
- [Usage](#usage)
- [Example Data](#example-data)

## Introduction

The sandbox API (`sandbox-api.dexcom.com`) allows developers public access to simulated CGM data, and as such provide a great use case when discussing how CGM data might be mapped to FHIR resources. This doc solely focuses on the Dexcom format of this data.

## Endpoints

### EGVs Endpoint

**Endpoint:** `/v3/users/self/egvs`

The `/egvs` endpoint provides estimated glucose values (EGVs) collected by the Dexcom CGM system.

**Key Parameters:**
- `startDate`: The start date/datetime for the data retrieval period (ISO 8601 format). Inclusive filter.
- `endDate`: The end date/datetime for the data retrieval period (ISO 8601 format). Exclusive filter.

### Devices Endpoint

**Endpoint:** `/v3/users/self/devices`

The `/devices` endpoint provides information about the devices used to collect CGM data.

## Data Models

### EGV Data

The EGV (Estimated Glucose Value) data includes the following fields:

- `recordId`: A unique ID for this particular EGV reading.
- `systemTime`: The timestamp when the data was recorded by the CGM system (UTC).
- `displayTime`: The timestamp when the data was displayed to the user (local time).
- `transmitterId`: A hashed value of the transmitter's serial number that produced the reading.
- `transmitterTicks`: A monotonically increasing counter representing the total number of seconds elapsed since the transmitter was first activated.
- `value`: The glucose value in mg/dL.
- `trend`: The trend of the glucose value (e.g., doubleDown, singleDown, flat, singleUp, doubleUp).
- `trendRate`: The rate of change of the glucose value (mg/dL/min).
- `status`: The status of the glucose value (e.g., high, low). Optional.
- `unit`: The unit the value was recorded in (e.g., mg/dL, mmol/L)
- `rateUnit`: The unit the rate of change of the value was recorded in (e.g., mg/dL/min, mmol/L/min)
- `displayDevice`: The device that the reading was initially displayed on (e.g. iOS, android, receiver)
- `transmitterGeneration`: The generation of the transmitter (e.g. g7, g6, g5, g4).

### Device Data

The device data includes detailed information about the devices used to generate/capture the readings:

- `lastUploadDate`: The last date and time when data was uploaded from the device (UTC).
- `transmitterId`: A hashed value of the transmitter's serial number that produced the reading.
- `transmitterGeneration`: The generation of the transmitter (e.g. g7, g6, g5, g4).
- `displayDevice`: The device that the reading was initially displayed on (e.g. iOS, android, receiver)
- `displayApp`: The application used to display the glucose readings (e.g., g7).
- `alertSchedules`: A list of alert schedules configured on the device, including:
  - `alertScheduleSettings`: Settings for each alert schedule, such as the name, enabled status, start and end times, days of the week, and override settings.
  - `alertSettings`: Specific alert settings within each schedule, including the alert name, value, unit, enabled status, system time, display time, secondary trigger condition, sound theme, and sound output mode.

## Usage

To use the Dexcom sandbox API, you need to authenticate and obtain an access token. Include this token in the `Authorization` header of your API requests. You can use the /dataRange endpoint to find valid start and end date values.

```bash
curl -X GET "https://sandbox-api.dexcom.com/v3/users/self/egvs?startDate=2023-01-01T00:00:00&endDate=2023-01-02T00:00:00" \
-H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Example Data
You can find example data in the following locations:

- [EGVs](./egv.json)
- [Devices](./device.json)
