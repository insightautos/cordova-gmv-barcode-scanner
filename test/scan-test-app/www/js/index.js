/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// Wait for the deviceready event before using any of Cordova's device APIs.
// See https://cordova.apache.org/docs/en/latest/cordova/events/events.html#deviceready
document.addEventListener('deviceready', onDeviceReady, false);

function onSuccess(result) {
  const node = document.createElement('div');
  node.textContent = `${result.text} (${result.format}/${result.type})`;
  document.getElementById('output').prepend(node);
}

function scan() {
  const formData = new FormData(document.querySelector('form'));
  const options = {};

  for (const pair of formData.entries()) {
    const key = pair[0];
    const value = pair[1];
    options[key] = value === 'true';
  }

  cordova.plugins.mlkit.barcodeScanner.scan(options, onSuccess, console.error);
}

function onDeviceReady() {
  console.log('Running cordova-' + cordova.platformId + '@' + cordova.version);
  document.getElementById('scan').onclick = scan;
}
