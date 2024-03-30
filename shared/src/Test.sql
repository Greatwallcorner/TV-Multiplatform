alter table address add column flag1 varchar(10);


explain update address set flag1 = '1' where  address_id in (1,2,180.600,60,504);

select count(1) from address;

