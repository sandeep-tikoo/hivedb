{
	"dimension":
    {
			"type":"INTEGER",
			"uri":"jdbc://uri",
			"resources":
      	[
        	{
          	"type":"INTEGER",
            "indexes":
            	[
              	{
                	"type":"VARCHAR",
                	"name":"anIndex",
                  "id":1
                 }
              ],
              "partition_resource":false,
              "name":"aResource",
              "id":1
            }
        	],
			"name":"aDimension",
      "id":0
    },
	"semaphore":
    {
    	"status":0,
      "revision":1
    },
	"nodes":
    [
    	{
      	"capacity":0,
	      "dialect":"H2",
	      "host":"localhost",
	      "port":0,
	      "name":"aNode",
	      "database":"nodeDb",
	      "id":0
      }
		]
}