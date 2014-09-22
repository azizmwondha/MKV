The Index servlet can receive the following query string parameters:
– nemid_clientmode: Chooses the client mode of the NemID JS client.
  Legal values: standard, limited.
  Default value: standard

– nemid_width: Width of the NemID JS client.
  Legal values: If parameter nemid_clientmode=standard, minimum 500px. If parameter nemid_clientmode=limited, minimum 200px (portrait) or 250px (landscape).
  Default value: If parameter nemid_clientmode=standard, 500px. If parameter nemid_clientmode=limited, 200px.

– nemid_height: Height of the NemID JS client.
  Legal values: If parameter nemid_clientmode=standard, minimum 450px. If parameter nemid_clientmode=limited, minimum 250px (portrait) or 200px (landscape).
  Default value: If parameter nemid_clientmode=standard, 450px. If parameter nemid_clientmode=limited, 250px.
