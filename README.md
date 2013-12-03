ScorrentScapp
=============

The sickest Scala Scapp son
The sickest yo


Hashing Library:
	Methods:
		hash: getHash(File)
		hash: getHash(ByteString)

BitMap Library:
	???	Need to determine the underlying data type.  Core operations should be very simple using the <<, &, and | operators but presumably they can only be used on primitives.  If BitMaps are stored as arrays of primitive types, then perhaps core operations can be implemented using functional Map or Fold operations.  Some research is needed.
	BitMap Class:
		Members:
			- ByteArray, Length
		Constructors:
			BitMap(ByteArray, Length)
				-> if values have been calculated
			BitMap()
				-> create an empty BitMap
		Methods:
			void: SetBit(Index, Value)
			bool: GetBit(Index)
				-> set or get the value of the Index'th bit of BitMap
			BitMap: operator &(BitMap Other)
				-> binary and
			BitMap: operator |(BitMap Other)
				-> binary or
			int: GetFirstIndex()
				-> basic function to return the first true bit of a BitMap
			int: GetNextIndex(Index)
				-> function to get the first true bit after a given index

Chunking Library:
	Chunk Class:
		Members:
			- ByteString pointer, Destination File offset, Chunk Hash, bool Exists, bool Loaded
		Constructors:
			Chunk(Bytestring pointer, DestFile Offset)
				-> when generating a new Scorrent from a file; should calculate Chunk Hash from the data
			Chunk(DestFile Offset, Chunk Hash, Exists Flag)
				-> when generating a ChunkArray for an existing Scorrent file
		Methods:
			void: LoadChunk()
				-> load data for an existing chunk into memory.  Throws an error if Exists is false.
			void: UnloadChunk()
				-> for memory management, to unload chunk data which is not being transferred anymore.
			void: WriteChunk(Bytestring pointer, Destinition File)
				-> write the data at Bytestring pointer to Destination File at DestFile Offset
			bool: VerifyChunk(Bytestring pointer)
				-> verifies that the signature of the data pointed to by Bytestring pointer matches the Chunk Hash

	ChunkArray Class:
		Members:
			- Chunk Count, chunkArray[Chunk Count], File Path, File Hash, bool Complete, Chunk Size, Chunks Owned (bitmap), Chunks Loaded (bitmap)
		Constructors:
			ChunkArray(File Path, Chunksize)
				-> when generating from a raw file.  Note that this operation should not load any Chunks or set the Loaded map.
			ChunkArray(Scorrent File, Destination File Path)
				-> when generating from a new Scorrent file
			ChunkArray(Scorrent File, File Path, bool Complete, Chunks Owned)
				-> when being generated at Client startup, for a pre-existing Scorrent file
		Methods:
			bool Verify()
				-> Final verification of a downloaded file; verifies that data written at File Path matches the signature of File Hash

Scorrent File Library:
	Scorrent file (.scor):
		- Filename, Hash, Tracker Address (IP, Port), Chunk Size, List of Chunks
		- List of Chunks:
			- Chunk ID, Chunk Hash
		Functions:
			void: WriteScorrentFile(ChunkArray, Tracker Address)
				-> write a Scorrent file to disk
			(File Hash, Chunk Size, Chunk[]): ReadScorrentFile(Scorrent File)
				-> Helper function for ChunkArrays being constructed from existing Scorrent files

Peer User Interface API for the GUI:
	ChunkArray: createNewScorrent(File Path, Tracker Address)
	ChunkArray: addScorrent(Scorrent Path, Destination Path)
	void: startScorrent(Filename)
		-> to start downloads for a Scorrent
	void: stopScorrent(Filename)
		-> to stop downloads for a Scorrent
	void: removeScorrent(Filename)
	void: changeDest(Filename, Destination Path)
	void: shutDown()

Peer-to-Peer Client Architecture:
	- The client stores all loaded Scorrent files in a private directory for persistent storage.  It also maintains a temporary 'Scorrent Status' file which stores, at the time of the client's last shutdown, each Scorrent's destination File Path, 'Completed' flag, "Chunks Owned" bitmap and whether it was active (i.e. downloading/uploading).

	Actor Classes:
		PeerMonitor
			-> maintains a list of peers for a particular Scorrent file.  In particular, if downloading, it pings the tracker every <Refresh Peerlist Period> seconds to keep an active list.
			-> Recieve Messages: PeerList, ConnectionClosed, RequestConnection
			-> Send Messages: RequestPeers, RequestConnection
		ChunkMonitor
			-> maintains the overall Chunk data for a particular Scorrent file.  In particular, it maintains the Chunks Owned and Chunks Loaded bitmaps.
			-> Recieve Messages: ChunkCompleted, RequestLoadChunk, ChangeDest
			-> Send Messages: OwnedMap, LoadedMap
		ConnectionMonitor
			-> maintains connection information between the client and a specific peer.  In particular, if downloading, it maintains Owned and Loaded bitmaps for the other peer and searches them to determine which Chunks to ask for.  However, ConnectionMonitor does not handle the data transfer itself; instead it oversees the DownChannel and UpChannel Actors for the connection.
			-> Recieve Messages: OwnedMap, LoadedMap, PeerOwned, PeerLoaded, RequestChunk
			-> Send Messages: RequestLoadChunk, PeerOwned, PeerLoaded, GetChunk, SendChunk, ConnectionClosed
		DownChannel
			-> handles raw chunk data coming over a connection.  Recieves GetChunk instructions from a ConnectionMonitor identifying which Chunks to request, sends RequestChunk messages to the connected peer, then saves the recieved data.
			-> Recieve Messages: GetChunk, ChunkData
			-> Send Messages: RequestChunk, ChunkCompleted
		UpChannel
			-> passes forward raw chunk data to peers which have requested it.
			-> Recieve Messages: SendChunk
			-> Send Messages: ChunkData
		ScorrentListMonitor
			-> maintains the client's list of active Scorrents.  It is responsible for the creation and termination of PeerMonitors and ChunkMonitors for every active file.
			-> Recieve Messages: StartScorrent, StopScorrent
		ClientStatusMonitor
			-> responsible for pinging the tracker every <Tracker Checkin Period> with the list of complete or partially complete Scorrents which it can upload data for.
			-> Recieve Messages: -
			-> Send Messages: StatusReport

	Actor Hierarchy:
		Actor System
			|
			|-----> ClientStatusMonitor
			|
			|-----> ScorrentListMonitor
			   |
			   |-----> PeerMonitor(s)
				  |
				  |---> ChunkMonitor
				  |
				  |-----> Connection Monitor
				  	 |
					 |---> DownChannel
					 |
					 |---> UpChannel

	Main():
		- Initialize the Actor System and process the stored list of Scorrent files.  For each active Scorrent, start up a PeerMonitor.
		- Register UI callbacks for the GUI, then wait.


Tracker Server Architecture
	- The tracker stores all registered Scorrent files in a private directory for persistent storage.

	Actor Classes:
		ScorrentListMonitor
			-> maintains the tracker's total list of Scorrents.  When first started up, processes the ScorrentFile directory to build the list.
			-> Recieve Messages: RegisterScorrent
			-> Send Messages: -
		ActiveScorrentsMonitor
			-> maintains the list of Scorrents with active peers.  Oversees the tracker's PeerListMonitors.
			-> Recieve Messages: StatusReport, GetPeerList
			-> Send Messages: PeerUpdate, PeerList
		PeerListMonitor
			-> keeps a list of peers for a particular Scorrent file, sorted by the each peer's latest check-in time.
			-> Recieve Messages: GetPeerList, PeerUpdate, PeerListUpdated
			-> Send Messages: PeerList, PeerUpdate
		PeerListUpdater
			-> sub-Actor to update a PeerListMonitor's peer list when it recieves a PeerUpdate report
			-> Recieve Messages: PeerUpdate
			-> Send Messages: PeerListUpdated
		HandlerPoolMonitor
			-> a supervisor for the ConnectionHandler pool.  Forwards incoming messages to a free ConnectionHandler.
			-> Recieve Messages: AddNewScorrent, StatusReport, RequestPeers
			-> Send Messages: AddNewScorrent, StatusReport, RequestPeers
		ConnectionHandler
			-> Generic connection handler which redirects the Tracker's incoming messages to the appropriate Actor.
			Commands:
				- AddNewScorrent -> ScorrentListMonitor ! RegisterScorrent
				- StatusReport -> ActiveScorrentsMonitor ! StatusReport
				- RequestPeers -> ActiveScorrentsMonitor ! GetPeerList -> (Recieve PeerList) -> Sender ! PeerList

	Actor Hierarchy:
		Actor System
			|
			|-----> ScorrentListMonitor
			|
			|-----> ActiveScorrentsMonitor
			|  |
			|  |-----> PeerListMonitor(s)
			|  	  |
			|	  |-----> PeerMonitorUpdater
			|
			|-----> HandlerPoolMonitor
			   |
			   |-----> ConnectionHandler(s)

	Main():
		- Initialize the Actor System.

