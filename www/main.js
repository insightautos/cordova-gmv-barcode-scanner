// JavaScript Document
CDV = (typeof CDV == 'undefined' ? {} : CDV);
var cordova = window.cordova || window.Cordova;
(function () {
    function GMVBarcodeScanner() { }
    GMVBarcodeScanner.prototype.scan = function (params, callback) {
        // Default settings. Scan every barcode type.
        var settings = {
            types: {
                Code128: true,
                Code39: true,
                Code93: true,
                CodaBar: true,
                DataMatrix: true,
                EAN13: true,
                EAN8: true,
                ITF: true,
                QRCode: true,
                UPCA: true,
                UPCE: true,
                PDF417: true,
                Aztec: true
            },
            detectorSize: {
                width: .5,
                height: .7
            }
        };
        for (var key in params) {
            if (params.hasOwnProperty(key) && settings.hasOwnProperty(key)) {
                settings[key] = params[key];
            }
        }
        // Support legacy API.
        if (settings.vinDetector) {
            return this.scanVIN(callback, settings.detectorSize);
        }
        var detectorTypes = 0;
        // GMVDetectorConstants values allow us to pass an integer sum of all the desired barcode types to the scanner.
        var detectionTypes = {
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
        };
        for (var key in settings.types) {
            if (detectionTypes.hasOwnProperty(key) && settings.types.hasOwnProperty(key) && settings.types[key] == true) {
                detectorTypes += detectionTypes[key];
            }
        }
        // Order of this settings object is critical. It will be passed in a basic array format and must be in the order shown.
        var stngs = {
            //Position 1
            detectorType: detectorTypes,
            //Position 2
            detectorWidth: settings.detectorSize.width,
            //Position 3
            detectorHeight: settings.detectorSize.height
        };
        var sendSettings = [];
        for (var key in stngs) {
            if (stngs.hasOwnProperty(key)) {
                sendSettings.push(stngs[key]);
            }
        }
        this.sendScanRequest(sendSettings, callback);
    };
    GMVBarcodeScanner.prototype.sendScanRequest = function (settings, callback) {
        callback = typeof callback == "function" ? callback : function () { };
        cordova.exec(function (data) {
            callback(null, data[0]);
        },
            function (err) {
                switch (err[0]) {
                    case null:
                    case "USER_CANCELLED":
                        callback({ cancelled: true, message: "The scan was cancelled." });
                        break;
                    case "SCANNER_OPEN":
                        callback({ cancelled: false, message: "Scanner already open." });
                        break;
                    default:
                        callback({ cancelled: false, message: err });
                        break;
                }
            }, 'cordova-plugin-barcode-detector', 'startScan', settings);
    };
    GMVBarcodeScanner.prototype.scanLicense = function (callback, settings) {
        var width = typeof settings != "undefined" && settings.width ? settings.width : .5,
            height = typeof settings != "undefined" && settings.height ? settings.height : .7;
        var that = this;
        // Send request with PDF417 scanning only.
        this.sendScanRequest([2048, width, height], function (err, data) {
            if (err) return callback(err);
            var licenseProcessingResult = that.processLicenseResult(data);
            // If processLicenseResult returns false then the scan isn't formatted correctly.
            if (licenseProcessingResult === false) return callback({ cancelled: false, message: "Invalid barcode encoding.", barcodeScan: data[0] });
            callback(null, licenseProcessingResult);
        });
    };
    GMVBarcodeScanner.prototype.scanVIN = function (callback, settings) {
        var width = typeof settings != "undefined" && settings.width ? settings.width : .5,
            height = typeof settings != "undefined" && settings.height ? settings.height : .7;
        // Send the request for VIN scanning only.
        this.sendScanRequest([0, width, height], callback);
    };
    GMVBarcodeScanner.prototype.processLicenseResult = function (result) {
        if (typeof result == "undefined") {
            return false;
        }
        var temp = result.split("ANSI ");
        if (temp.length > 1) {
            temp = "ANSI " + temp[1];
        } else {
            // If there's no ANSI near the beginning of the file and something
            // after it then we assume the scan is invalid.
            return false;
        }
        var slices = {
            "ANSI": 5,
            "issueIdentificationNumber": 6,
            "AAMVAVersion": 2,
            "jurisdictionVersionNumber": 2,
            "numberOfEntries": 2
        };
        var data = {},
            i = 0;
        for (var k in slices) {
            if (slices.hasOwnProperty(k)) {
                data[k] = temp.slice(i, i + slices[k]);
                i += slices[k];
            }
        }
        // If AAMVA version is 1 then the offsets in the beginning are slightly different and
        // we need to ignore the jurisdictionVersionNumber since it has yet to be introduced.
        if (data.AAMVAVersion == "01") {
            data.numberOfEntries = data.jurisdictionVersionNumber;
            delete (data.jurisdictionVersionNumber);
            i -= 2;
        }
        // Number of "subfiles" in the scan.
        data.numberOfEntries = +data.numberOfEntries;
        var subfileTypes = {
            "type": 2,
            "offset": 4,
            "length": 4
        };
        // Pull out each subfile. The subfiles are defined with a start and end index against the entire scanned
        // dataset so we have to pull out that exact length. Be careful with test cases to verify that it is in fact a valid AAMVA format.
        var subfiles = [];
        for (var a = 0; a < data.numberOfEntries; a++) {
            var subfile = {};
            for (var k in subfileTypes) {
                if (subfileTypes.hasOwnProperty(k)) {
                    subfile[k] = temp.slice(i, i + subfileTypes[k]);
                    i += subfileTypes[k];
                }
            }
            subfile.offset = +subfile.offset;
            subfile.length = +subfile.length;
            subfile.data = result.slice(subfile.offset, subfile.offset + subfile.length);
            if (subfile.data.indexOf(subfile.type) === 0) {
                subfile.data = subfile.data.slice(2);
            }
            subfiles.push(subfile);
        }
        // Get only the subfiles that are of type DL or ID. Some jurisdiction specific subfile types might be here that we don't care about.
        subfiles = subfiles.filter(function (f) {
            return f.type == "DL" || f.type == "ID";
        });
        // If there are no subfiles then the scan is considered invalid.
        if (subfiles.length == 0) {
            return false;
        } else {
            // If there are subfiles we take the first one and split it into the separate lines.
            result = subfiles[0].data.split("\n")
        }

        // Handle the different AAMVA versions. There are some weird things that happened in the first AAMVA version where the
        // first, middle, and last name were all combined into the DAA field. Because of this we cannot acceptably extract the
        // names into their appropriate fields so the FirstName/MiddleName/LastName fields are not necessarily accurate in version 1.
        // As well, the date format was changed for the birth date and license expiration from format 1 to 2.
        var dateFormat = "MMDDYYYY";
        var map;
        switch (data.AAMVAVersion) {
            case "01":
                map = {
                    DBA: "LicenseExpiration",
                    DAA: "Names",
                    DBB: "BirthDate",
                    DAG: "Address.Address",
                    DAI: "Address.City",
                    DAJ: "Address.State",
                    DAK: "Address.Zip",
                    DAQ: "LicenseNumber"
                };
                dateFormat = "YYYYMMDD";
                break;
            case "02":
            case "03":
                map = {
                    DBA: "LicenseExpiration",
                    DCS: "LastName",
                    DCT: "GivenNames",
                    DBB: "BirthDate",
                    DAG: "Address.Address",
                    DAI: "Address.City",
                    DAJ: "Address.State",
                    DAK: "Address.Zip",
                    DAQ: "LicenseNumber"
                };
                break;
            case "04":
            case "05":
            case "06":
            case "07":
            case "08":
            case "09":
            default:
                map = {
                    DBA: "LicenseExpiration",
                    DCS: "LastName",
                    DAC: "FirstName",
                    DAD: "MiddleName",
                    DBB: "BirthDate",
                    DAG: "Address.Address",
                    DAI: "Address.City",
                    DAJ: "Address.State",
                    DAK: "Address.Zip",
                    DAQ: "LicenseNumber"
                };
                break;
        }
        var c = {};
        for (var i = 0; i < result.length; i++) {
            var line = result[i],
                code = line.slice(0, 3),
                val = line.slice(3);
            if (map.hasOwnProperty(code)) {
                switch (code) {
                    // Run names and addresses through the capitalizeString function for cleaner formatting.
                    case "DCS":
                    case "DAC":
                    case "DAD":
                    case "DAG":
                    case "DAI":
                        val = this.capitalizeString(val);
                        break;
                    // Parse BirthDate and LicenseExpiration according to the defined date format depending on the the AAMVA version.
                    case "DBB":
                    case "DBA":
                        var d;
                        switch (dateFormat) {
                            case "YYYYMMDD":
                                d = new Date(parseInt(val.slice(0, 4)), parseInt(val.slice(4, 6)) - 1, parseInt(val.slice(6, 8)));
                                break;
                            case "MMDDYYYY":
                                d = new Date(parseInt(val.slice(4, 8)), parseInt(val.slice(0, 2)) - 1, parseInt(val.slice(2, 4)));
                                break;
                        }
                        var year = d.getFullYear(),
                            month = d.getMonth() + 1,
                            day = d.getDate();
                        val = month + "/" + day + "/" + year;
                        break;
                    // Get first 5 digits off Zip in case of longer Zip code.
                    case "DAK":
                        val = val.slice(0, 5);
                        break;
                    // In the case of AAMVA versions 2 and 3 we assume the first word is the first name and any words after are part of the middle name.
                    case "DCT":
                        val = val.split(" ");
                        if (val.length > 0) {
                            c['FirstName'] = this.capitalizeString(val[0]);
                        }
                        if (val.length > 1) {
                            c['MiddleName'] = this.capitalizeString(val.slice(1).join(" "));
                        }
                        continue;
                    // In the case of AAMVA version 1 we assume the first word is the first name, second word is second name, and any remaining words are the last name.
                    case "DAA":
                        val = val.split(",");
                        if (val.length > 0) {
                            c['FirstName'] = this.capitalizeString(val[0]);
                        }
                        if (val.length > 1) {
                            c['MiddleName'] = this.capitalizeString(val.slice(1, val.length - 1).join(" "));
                        }
                        if (val.length > 1) {
                            c['LastName'] = this.capitalizeString(val.slice(val.length - 1, val.length)[0]);
                        }
                        continue;
                }
                var path = map[code].split(".");
                if (path.length > 1) {
                    if (typeof c[path[0]] != "object") { c[path[0]] = {}; }
                    c[path[0]][path[1]] = val;
                } else {
                    c[path[0]] = val;
                }
            }
        }
        //Since there's no other state designation in the license scans except from the address so we assign it that way.
        if (c.Address && c.Address.State) {
            c.LicenseState = c.Address.State;
        }
        return c;
    };
    GMVBarcodeScanner.prototype.capitalizeString = function (string) {
        var wordSplitters = [" ", "-", "O'", "L'", "D'", "St.", "Mc", "Mac"],
            lowercaseExceptions = ['the', 'van', 'den', 'von', 'und', 'der', 'de', 'da', 'of', 'and', "l'", "d'"],
            uppercaseExceptions = ['II', 'III', 'IV', 'VI', 'VII', 'VIII', 'IX'];
        string = string.toLowerCase();
        //Iterate through each word splitter and see if there are any words that have that "splitter" in them.
        for (var i1 = 0; i1 < wordSplitters.length; i1++) {
            var delimiter = wordSplitters[i1],
                words = string.split(delimiter),
                // Define the collection of final words that will be used.
                newWords = [];
            // Iterate through words that were split by the splitter and check if an upper or lowercase
            // exemption exists. If it does, respect them, otherwise capitalize the first letter of the word.
            for (var i2 = 0; i2 < words.length; i2++) {
                var word = words[i2];
                if (uppercaseExceptions.indexOf(word.toUpperCase()) > -1) {
                    word = word.toUpperCase();
                } else if (lowercaseExceptions.indexOf(word.toLowerCase()) === -1) {
                    word = word.charAt(0).toUpperCase() + word.slice(1);
                }
                newWords.push(word);
            }
            // Force lowercase exemptions to lower.
            if (lowercaseExceptions.indexOf(delimiter.toLowerCase()) > -1) {
                delimiter = delimiter.toLowerCase();
            }
            //Combine the final words. This is inside the for loop so each word splitter is handled separately.
            string = newWords.join(delimiter);
        }
        return string;
    };
    module.exports = new GMVBarcodeScanner();
})();
