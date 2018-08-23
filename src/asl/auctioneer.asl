// Holder of the auction
// used for checking auction interaction

{include("tracker.asl")}

//target(3,3).
noNeighbors(2).
tracking('auctioneer', 1, 5, 0).
canSee(0,0,6,5).
myPosition(0,0).
//target(6,6).
losingTarget('auctioneer',1,5,0).
