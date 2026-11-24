import Observation
import Shared

/// Makes a shared ViewModel observable by SwiftUI and gives it an owner.
///
///     let model = Observed(graph.loginViewModel(), state: \.state)
///
/// - `state` is the latest value published by the ViewModel. SwiftUI redraws when it changes.
/// - `send` calls the ViewModel and refreshes `state` before it returns, so a text field
///   never shows a value older than what the user just typed.
/// - When the last reference goes away, the ViewModel is cleared and its coroutines are
///   cancelled. On Android an activity or a navigation entry plays this role.
///
/// The state flow must hold a non-optional value: a screen always has a state.
@MainActor
@Observable
public final class Observed<VM: ViewModel, UiState> {

    public let viewModel: VM
    public private(set) var state: UiState

    @ObservationIgnored private let flow: SkieSwiftStateFlow<UiState>
    @ObservationIgnored private let lifetime: ViewModelLifetime
    @ObservationIgnored private var updates: Task<Void, Never>?

    public init(_ viewModel: VM, state: KeyPath<VM, SkieSwiftStateFlow<UiState>>) {
        let flow = viewModel[keyPath: state]
        self.viewModel = viewModel
        self.flow = flow
        self.state = flow.value
        self.lifetime = ViewModelLifetime(viewModel: viewModel)
        self.updates = Task { [weak self] in
            // Changes the ViewModel makes on its own, for example when a request ends.
            for await value in flow {
                self?.state = value
            }
        }
    }

    /// Calls the ViewModel, then reads its new state at once.
    public func send(_ action: (VM) -> Void) {
        action(viewModel)
        state = flow.value
    }

    deinit {
        updates?.cancel()
        lifetime.close()
    }
}
