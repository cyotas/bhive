#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include "DHT.h"

const char* ssid = "InfAP";                      //wifi name (InfAP)
const char* pwd = "informatik";                  //wifi password (informatik)
const char* server = "http://pi-1.local:4044/";  //http server & port (http://pi-1:4044/)

const int deepSleepSec = 20;	//deep sleep time in seconds

DHT dht_inside(8, DHT22);    //sensor inside pin 8
DHT dht_outside(10, DHT22);  //sensor outside pin 10

void setup() {
  long startMS = millis();

  dht_inside.begin();
  dht_outside.begin();

  Serial.begin(115200);
  Serial.println("Wakeup");

  pinMode(4, OUTPUT);
  pinMode(5, OUTPUT);
  digitalWrite(5, HIGH);

  WiFi.setHostname("Beehive");
  WiFi.begin(ssid, pwd);
  while (WiFi.status() != WL_CONNECTED) {}
  Serial.println(WiFi.localIP());

  digitalWrite(5, LOW);
  digitalWrite(4, HIGH);

  delay(5000);

  float it = dht_inside.readTemperature();
  float ih = dht_inside.readHumidity();
  float ot = dht_outside.readTemperature();
  float oh = dht_outside.readHumidity();

  while(isnan(it) || isnan(ih) || isnan(ot) || isnan(oh)) {
    Serial.print(".");
    it = dht_inside.readTemperature();
    ih = dht_inside.readHumidity();
    ot = dht_outside.readTemperature();
    oh = dht_outside.readHumidity();
  }

  StaticJsonDocument<64> jsonDoc;
  jsonDoc["in_temp"] = it;
  jsonDoc["in_humi"] = ih;
  jsonDoc["out_temp"] = ot;
  jsonDoc["out_humi"] = oh;

  String jsonOutput;
  serializeJson(jsonDoc, jsonOutput);
  Serial.println(jsonOutput);

  WiFiClient client;
  HTTPClient http;
  http.begin(client, server);
  http.addHeader("Content-Type", "application/json");
  http.POST(jsonOutput);
  http.end();

  Serial.println("Deep Sleep");
  Serial.flush();
  esp_sleep_enable_timer_wakeup((deepSleepSec - (float)(millis() - startMS) / 1000) * 1000000ULL);
  esp_deep_sleep_start();
}

void loop() {}