INSERT INTO country (id,name,timetableUrlTemplate,email,overrideLicense,active) VALUES
	 ('de','Deutschland','https://mobile.bahn.de/bin/mobil/bhftafel.exe/dox?bt=dep&max=10&rt=1&use_realtime_filter=1&start=yes&input={title}','info@railway-stations.org',NULL,true),
	 ('ch','Schweiz','http://fahrplan.sbb.ch/bin/stboard.exe/dn?input={title}&REQTrain_name=&boardType=dep&time=now&maxJourneys=20&selectDate=today&productsFilter=1111111111&start=yes','fotos@schweizer-bahnhoefe.ch',NULL,true),
	 ('it','Repubblica Italiana',NULL,'info@railway-stations.org',NULL,false),
	 ('se','Konungariket Sverige',NULL,'info@railway-stations.org',NULL,false);
