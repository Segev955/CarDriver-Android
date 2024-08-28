# CarDriver-Android

CarDriver-Android is part of the **Car Driver Project**, a comprehensive solution to detect vehicle theft by analyzing driver behavior. This Android app interacts with OBD (On-Board Diagnostics) data, retrieves information in real-time, and uses machine learning models to identify the driver based on their driving style.

## Features

- Real-time OBD-II data retrieval from Firebase
- Machine learning integration for driver identification
- Theft detection using driver behavior patterns
- Map visualization using OpenStreetMap (OSM) for displaying vehicle locations
- Notification handling for alerts
- User authentication and device management via Firebase

## Technologies Used

- **Android (Kotlin)**: Core of the application
- **Firebase**: Real-time database for storing and retrieving data
- **TensorFlow**: Machine learning framework for the driver identification algorithm
- **OpenStreetMap (OSM)**: Display vehicle locations on the map

## How to Use

1. **Clone the repository**:
    ```bash
    git clone https://github.com/Segev955/CarDriver-Android.git
    ```

2. **Install necessary dependencies**:
   The app uses Gradle to manage dependencies. Open the project in Android Studio, and it will automatically install the required packages.

3. **Set up Firebase**:
    - Add your Firebase configuration file (`google-services.json`) to the `app/` directory.
    - Configure Firebase Realtime Database to store OBD-II data and device information.

4. **Running the App**:
    - Connect your Android device and run the app from Android Studio.
    - Use the provided UI to connect to your OBD-II device and retrieve real-time data.

## Project Structure

- `app/src`: Contains the main Kotlin code for the Android app.
- `app/src/main/res`: Contains layout files and resources.
- `Firebase/`: Contains the logic for handling notifications and database interactions.
- `OpenStreetMap/`: Implements the map functionality using OSM.

## Contributors

- **Segev Tzabar**
- **Yasmin Cohen**
- **Sali Sharfman**
