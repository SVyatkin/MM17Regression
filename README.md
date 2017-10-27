Hackaton Minds + Machines 2017 

https://github.com/PredixDev/minds-machines-sf


/ts/data/{name} - init power data by region name

/ts/temp/{name} - init temparture data by region

/ts/predict/{name} 

POST forecast request by hours, days and weeks



Example to get forecast for 5 hours in 7-Nov-2017 12:00

https://regression.run.aws-usw02-pr.ice.predix.io/ts/predict/Dromore

{
   "unit":"hr",
   
   "times":5,
   
   "startDate":"7-Nov-2017 12:00",
   
   "temperatures":[12.2,11.9,11,10,9]
   
   }
   
   
   response 
   
[

    {
        "timestamp": 1510056000000,
        "value": 650.9032418988782
    },
    
    {
        "timestamp": 1510059600000,
        "value": 601.7486928623142
    },
    
    {
        "timestamp": 1510063200000,
        "value": 606.9424226947721
    },
    
    {
        "timestamp": 1510066800000,
        "value": 629.7956625918184
    },
    
    {
        "timestamp": 1510070400000,
        "value": 620.5548866626658
    }
    
]

Example

   http://localhost:8080/ts/temp/test
   
   http://localhost:8080/ts/data/test
   
   Post to 
   
   http://localhost:8080/ts/predict/test
   
