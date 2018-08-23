// Holder of the auction
// used for checking auction interaction

{include("tracker.asl")}

//target(3,3).
canSee(5,0,10,6).
noNeighbors(2).
myPosition(10,0).
//target(6,6).
tracking('auctioneer2', 1, 5, 5).
