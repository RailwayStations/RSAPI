INSERT INTO "user" (id,name,email,url,ownPhotos,anonymous,license,"key",admin,emailVerification,sendNotifications,locale) VALUES
	 (1,'@user0',NULL,'',true,true,'CC0_10',NULL,false,NULL,true,NULL),
	 (2,'@user1',NULL,'',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (3,'@user2',NULL,'http://www.example.com/user2',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (4,'@user3',NULL,'https://www.example.com/user3',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (5,'@user4',NULL,'http://www.example.com/user4',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (6,'@user5',NULL,'',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (7,'@user6',NULL,'https://www.example.com/user6',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (8,'@user7',NULL,'https://www.example.com/use7',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (9,'@user8',NULL,'https://www.example.com/user8',true,false,'CC0_10',NULL,true,NULL,true,NULL),
	 (10,'@user9',NULL,'https://www.example.com/user9',true,false,'CC0_10',NULL,false,NULL,true,NULL);
INSERT INTO "user" (id,name,email,url,ownPhotos,anonymous,license,"key",admin,emailVerification,sendNotifications,locale) VALUES
	 (11,'@user10','user10@example.com','https://www.example.com/user10',true,false,'CC0_10','246172676F6E32696424763D3139246D3D36353533362C743D322C703D312432634C6B4C6949415958584E52742B6C7A3062614541242B706C7463365A4371386D534551772B4139374B304E37544B386A7072582F774141614B525933456D783400000000000000000000000000000000000000000000000000000000000000',true,'VERIFIED',true,NULL),
	 (12,'@user11',NULL,'https://www.example.com/user11',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (13,'@user12',NULL,'https://www.example.com/user12',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (14,'@user13',NULL,'',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (15,'@user14','user14@example.com','https://www.example.com/user14',true,false,'CC0_10','246172676F6E32696424763D3139246D3D36353533362C743D322C703D3124674B6B4C4644564D566E75436B4A64526F547838437724396E4E5A763970415750532F41456549456F59702F7A377635796B3270787357395057705053444633723400000000000000000000000000000000000000000000000000000000000000',true,'VERIFIED',true,NULL),
	 (16,'@user15',NULL,'',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (17,'@user16',NULL,'https://www.example.com/user16',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (18,'@user17',NULL,'https://www.example.com/user17',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (19,'@user18',NULL,'https://www.example.com/user18',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (20,'@user19',NULL,'https://www.example.com/user19',true,false,'CC0_10',NULL,false,NULL,true,NULL);
INSERT INTO "user" (id,name,email,url,ownPhotos,anonymous,license,"key",admin,emailVerification,sendNotifications,locale) VALUES
	 (21,'@user20',NULL,'https://www.example.com/user20',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (22,'@user21','user21@example.com','https://www.example.com/user21',true,false,'CC0_10','246172676F6E32696424763D3139246D3D36353533362C743D322C703D312432634C6B4C6949415958584E52742B6C7A3062614541242B706C7463365A4371386D534551772B4139374B304E37544B386A7072582F774141614B525933456D783400000000000000000000000000000000000000000000000000000000000000',false,NULL,true,NULL),
	 (23,'@user22',NULL,'https://www.example.com/user22',true,false,'CC BY-NC-SA 3.0 DE',NULL,false,NULL,true,NULL),
	 (24,'@user23',NULL,'https://www.example.com/user23',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (25,'@user24',NULL,'https://www.example.com/user24',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (26,'@user25',NULL,'',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (27,'@user26',NULL,'https://www.example.com/user26',true,false,'CC0_10',NULL,false,NULL,true,NULL),
	 (28,'@user27',NULL,'https://www.example.com/user27',true,false,'CC0_10','246172676F6E32696424763D3139246D3D36353533362C743D322C703D3124426D4F637165757646794E44754132726B566A6A3177246A7568362F6E6C2F49437A4B475570446E6B674171754A304F7A486A62694F587442542F2B62584D49476300000000000000000000000000000000000000000000000000000000000000',false,NULL,true,NULL);

SELECT setval('users_seq', 28);
