param(
    [Parameter(Mandatory = $true)]
    [string]$Command,
    [string]$Password = "lastworld-local-test",
    [string]$HostName = "127.0.0.1",
    [int]$Port = 25575
)

$ErrorActionPreference = "Stop"

function Read-Exact([IO.Stream]$Stream, [int]$Length) {
    $buffer = [byte[]]::new($Length)
    $offset = 0
    while ($offset -lt $Length) {
        $read = $Stream.Read($buffer, $offset, $Length - $offset)
        if ($read -le 0) { throw "RCON connection closed while reading a packet." }
        $offset += $read
    }
    return $buffer
}

function Write-Packet([IO.Stream]$Stream, [int]$RequestId, [int]$Type, [string]$Payload) {
    $payloadBytes = [Text.Encoding]::UTF8.GetBytes($Payload)
    $length = 4 + 4 + $payloadBytes.Length + 2
    $packet = [byte[]]::new(4 + $length)
    [BitConverter]::GetBytes($length).CopyTo($packet, 0)
    [BitConverter]::GetBytes($RequestId).CopyTo($packet, 4)
    [BitConverter]::GetBytes($Type).CopyTo($packet, 8)
    $payloadBytes.CopyTo($packet, 12)
    $Stream.Write($packet, 0, $packet.Length)
    $Stream.Flush()
}

function Read-Packet([IO.Stream]$Stream) {
    $lengthBytes = Read-Exact $Stream 4
    $length = [BitConverter]::ToInt32($lengthBytes, 0)
    if ($length -lt 10 -or $length -gt 1048576) { throw "Invalid RCON packet length: $length" }
    $body = Read-Exact $Stream $length
    $requestId = [BitConverter]::ToInt32($body, 0)
    $type = [BitConverter]::ToInt32($body, 4)
    $payloadLength = $length - 10
    $payload = if ($payloadLength -gt 0) { [Text.Encoding]::UTF8.GetString($body, 8, $payloadLength) } else { "" }
    return [pscustomobject]@{ RequestId = $requestId; Type = $type; Payload = $payload }
}

$client = [Net.Sockets.TcpClient]::new()
try {
    $client.Connect($HostName, $Port)
    $stream = $client.GetStream()
    $stream.ReadTimeout = 60000
    $stream.WriteTimeout = 60000

    Write-Packet $stream 1001 3 $Password
    $auth = Read-Packet $stream
    if ($auth.RequestId -eq -1) { throw "RCON authentication failed." }

    Write-Packet $stream 1002 2 $Command
    $response = Read-Packet $stream
    if ($response.RequestId -ne 1002) { throw "Unexpected RCON response id: $($response.RequestId)" }
    $response.Payload
} finally {
    $client.Dispose()
}
