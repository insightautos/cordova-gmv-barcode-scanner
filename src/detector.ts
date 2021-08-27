export const detectorFormat = Object.freeze({
  Code128: 1,
  Code39: 2,
  Code93: 4,
  CodaBar: 8,
  DataMatrix: 16,
  EAN13: 32,
  EAN8: 64,
  ITF: 128,
  QRCode: 256,
  UPCA: 512,
  UPCE: 1024,
  PDF417: 2048,
  Aztec: 4096
});

export const detectorType = Object.freeze({
  CONTACT_INFO: 1,
  EMAIL: 2,
  ISBN: 3,
  PHONE: 4,
  PRODUCT: 5,
  SMS: 6,
  TEXT: 7,
  URL: 8,
  WIFI: 9,
  GEO: 10,
  CALENDAR_EVENT: 11,
  DRIVER_LICENSE: 12
});
