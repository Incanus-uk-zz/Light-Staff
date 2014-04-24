#include "LPD8806.h"
#include "SPI.h"

int leds=8; // 8 for test 38 for staff

LPD8806 strip = LPD8806(leds); 

void setup() {
  strip.begin();
  strip.show();
  Serial.begin(115200);
  setStripColour(5, 5, 5);
}

void loop() {
  if (Serial.available() >= 1){
    int command = Serial.read();
    switch (command){
      case 0:
        singleColour();
        break;
      case 1:
        smoothTrans();
        break;
      case 2:
        swipe();
        break;
      case 3:
        chase();        // Not finished
        break;
      case 4:
        justData();     //How much can sit on the buffer??
        break;
      case 5:           // Wait(ms)
        delay(Serial.read());
        break;
      default:
        setStripColour(0, 10, 0);
    }    
  }    
}

void singleColour() {
  while (true){  
    if (Serial.available() >= 3){
      byte r = Serial.read();
      byte g = Serial.read();
      byte b = Serial.read();
      setStripColour(r, g, b);
      break; 
    }    
  }
}

void smoothTrans(){
  sendStaffState();
  while (true){  
    if (Serial.available() >= 1){
      int res = Serial.read();
      int counter = 0;
      int j;
      while (counter <= res){
        while (true){
            Serial.write(1);
          if (Serial.available() >= leds*3){
            for (j=0; j<leds; j++){
              byte rNew = Serial.read();
              byte gNew = Serial.read();
              byte bNew = Serial.read();
              strip.setPixelColor(j, rNew, gNew, bNew);
            }
            strip.show();
            break;
          }
        }
        counter++;
      }
      break;
    } 
  }    
}

void swipe(){    // Speed, out or in, r, g, b
  int doneFlag = 0;
  int j;
  int counter = 0;
  while (doneFlag == 0){  
    if (Serial.available() >= 5){
      
      double patSpeed = (double)Serial.read();
      byte outorin = Serial.read();
      byte r = Serial.read();
      byte g = Serial.read();
      byte b = Serial.read();
      
      for (j=0; j < leds/2;j++) {
        if (outorin == 0){
          strip.setPixelColor(j,r,g,b);
          strip.setPixelColor(leds-j-1,r,g,b);    
        } 
        else if (outorin == 1) {
          strip.setPixelColor(leds/2-j-1,r,g,b);
          strip.setPixelColor(leds/2+j,r,g,b);       
        }
        strip.show();
        delay (2 * patSpeed/(leds) * 100);
     }
     doneFlag = 1; 
    }    
  }

}

void chase(){
}

void justData() { //one staffs worth at a time
  int i;
  int f;
  int frames;
  while (true){
    if (Serial.available() >= 1){
      frames = Serial.read();
      break;
    };
  };
  for (f=0; f<frames; f++){
    for (i=0; i<leds; i++){
      while (true){  
        if (Serial.available() >= 3){
          byte r = Serial.read();
          byte g = Serial.read();
          byte b = Serial.read();
          strip.setPixelColor(i,r,g,b);
          break;
        }
      }
    }
    strip.show();
  }
}

void setStripColour(byte r, byte g, byte b){
  int i;
  for(i=0; i<strip.numPixels(); i++){
    strip.setPixelColor(i, r, g, b);
  }
  strip.show();
}

void sendStaffState(){
  Serial.write(leds);
  int j;  
  for (j=0; j<leds; j++){
    uint32_t rgb = strip.getPixelColor(j);
    byte r1 = (rgb >>  8) & 0x7f;
    byte g1 = (rgb >> 16) & 0x7f;
    byte b1 = (rgb >> 0) & 0x7f;
    Serial.write(r1);
    Serial.write(g1);
    Serial.write(b1);
  } 
}
